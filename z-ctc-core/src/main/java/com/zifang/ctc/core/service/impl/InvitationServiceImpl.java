package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.InvitationDO;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.service.InvitationDbService;
import com.zifang.ctc.core.service.InvitationService;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.core.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import com.zifang.util.core.lang.RandomUtil;

/**
 * 邀请服务实现类.
 * <p>
 * 邀请码生成 + 邀请接受/撤销/使用次数管理 + 业务校验,
 * 持久化委托给 {@link InvitationDbService}.
 *
 * @author zifang
 * @since 1.0.0
 * @see InvitationService
 * @see InvitationDbService
 * @see MembershipService
 * @see UserService
 */
@Service
public class InvitationServiceImpl implements InvitationService {

    private static final Logger log = LogManager.getLogger(InvitationServiceImpl.class);

    /**
     * 邀请码字符集 (去除 I/O/0/1 避免歧义).
     */
    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;
    private static final int DEFAULT_EXPIRE_DAYS = 7;

    private final InvitationDbService invitationDbService;
    private final MembershipService membershipService;
    private final UserService userService;

    public InvitationServiceImpl(InvitationDbService invitationDbService,
                                 MembershipService membershipService,
                                 UserService userService) {
        this.invitationDbService = invitationDbService;
        this.membershipService = membershipService;
        this.userService = userService;
    }

    /**
     * 创建邀请.
     *
     * @param tenantCode   租户编码
     * @param inviterUserId 邀请人用户ID
     * @param inviteeEmail  被邀请人邮箱（可选）
     * @param inviteePhone  被邀请人手机号（可选）
     * @param roleCode      角色编码（为空则默认为 "member"）
     * @param maxUses       最大使用次数（为空则默认为 1）
     * @param type          邀请类型（"link" 或 "code"）
     * @return InvitationDO 邀请记录
     * @throws IllegalArgumentException 当 tenantCode 或 inviterUserId 为空时
     */
    @Override
    @Transactional
    public InvitationDO create(String tenantCode, Long inviterUserId, String inviteeEmail,
                               String inviteePhone, String roleCode, Integer maxUses, String type) {
        if (tenantCode == null) { throw new IllegalArgumentException("tenantCode 必填"); }

        if (inviterUserId == null) { throw new IllegalArgumentException("inviterUserId 必填"); }


        InvitationDO inv = new InvitationDO();
        inv.setTenantCode(tenantCode);
        inv.setInviterUserId(inviterUserId);
        inv.setInviteeEmail(inviteeEmail);
        inv.setInviteePhone(inviteePhone);
        inv.setRoleCode(roleCode != null ? roleCode : "member");
        inv.setStatus(0);  // 待接受
        inv.setMaxUses(maxUses != null && maxUses > 0 ? maxUses : 1);
        inv.setUsedCount(0);
        inv.setExpireAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRE_DAYS));

        String t = type == null ? "link" : type;
        if ("code".equals(t)) {
            inv.setInviteCode(generateUniqueInviteCode());
        } else {
            inv.setToken(RandomUtil.uuidCompact());
        }

        inv.setCreatedAt(LocalDateTime.now());
        invitationDbService.insert(inv);
        log.info("InvitationServiceImpl.create 成功: id={}, type={}, tenant={}, inviter={}",
                inv.getId(), t, tenantCode, inviterUserId);
        return inv;
    }

    /**
     * 通过 Token 接受邀请（邮箱/链接邀请）.
     *
     * @param token        邀请 Token
     * @param acceptorUserId 接受人用户ID
     * @return MembershipDO 创建的租户关系
     * @throws IllegalArgumentException 当邀请不存在、已使用、已过期或 acceptorUserId 为空时
     * @throws IllegalStateException    当邀请已过期时
     */
    @Override
    @Transactional
    public MembershipDO acceptByToken(String token, Long acceptorUserId) {
        InvitationDO inv = invitationDbService.selectByToken(token);
        validateAcceptable(inv);
        MembershipDO ms = doAccept(inv, acceptorUserId);
        log.info("InvitationServiceImpl.acceptByToken 成功: invitationId={}, userId={}, tenant={}",
                inv.getId(), acceptorUserId, inv.getTenantCode());
        return ms;
    }

    /**
     * 通过邀请码接受邀请（Code 邀请）.
     *
     * @param code           邀请码
     * @param acceptorUserId 接受人用户ID
     * @return MembershipDO 创建的租户关系
     * @throws IllegalArgumentException 当邀请不存在、已使用、已过期或 acceptorUserId 为空时
     * @throws IllegalStateException    当邀请码已用完时
     */
    @Override
    @Transactional
    public MembershipDO redeemByCode(String code, Long acceptorUserId) {
        InvitationDO inv = invitationDbService.selectByInviteCode(code);
        validateAcceptable(inv);
        if (inv.getUsedCount() != null && inv.getMaxUses() != null
                && inv.getUsedCount() >= inv.getMaxUses()) {
            throw new IllegalStateException("邀请码已用完");
        }
        MembershipDO ms = doAccept(inv, acceptorUserId);
        log.info("InvitationServiceImpl.redeemByCode 成功: code={}, userId={}, tenant={}",
                code, acceptorUserId, inv.getTenantCode());
        return ms;
    }

    /**
     * 验证邀请是否可接受.
     *
     * @param inv 邀请记录
     * @throws IllegalArgumentException 当邀请不存在时
     * @throws IllegalStateException    当邀请已使用、已撤销、已过期时
     */
    private void validateAcceptable(InvitationDO inv) {
        if (inv == null) { throw new IllegalArgumentException("邀请不存在"); }

        if (inv.getStatus() == null || inv.getStatus() != 0) {
            throw new IllegalStateException("邀请已使用 / 已撤销 / 已过期");
        }
        if (inv.getExpireAt() != null && inv.getExpireAt().isBefore(LocalDateTime.now())) {
            inv.setStatus(3);
            invitationDbService.updateById(inv);
            throw new IllegalStateException("邀请已过期");
        }
    }

    /**
     * 执行邀请接受逻辑（创建 membership + 更新邀请状态）.
     *
     * @param inv            邀请记录
     * @param acceptorUserId 接受人用户ID
     * @return MembershipDO 创建的租户关系
     * @throws IllegalArgumentException 当 acceptorUserId 为空时
     */
    private MembershipDO doAccept(InvitationDO inv, Long acceptorUserId) {
        if (acceptorUserId == null) { throw new IllegalArgumentException("acceptorUserId 必填"); }

        // 校验 acceptor user 存在
        Optional<UserDO> userOpt = userService.findById(acceptorUserId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }
        // 建立 membership
        MembershipDO ms = membershipService.create(acceptorUserId, inv.getTenantCode(), inv.getRoleCode(), null);
        // 更新 invitation 状态
        boolean isCodeType = inv.getInviteCode() != null;
        if (isCodeType) {
            inv.setUsedCount((inv.getUsedCount() == null ? 0 : inv.getUsedCount()) + 1);
            if (inv.getMaxUses() != null && inv.getUsedCount() >= inv.getMaxUses()) {
                inv.setStatus(1);  // 已接受 (用完)
            }
        } else {
            inv.setStatus(1);  // 链接一次性, 直接标记已接受
        }
        inv.setAcceptedAt(LocalDateTime.now());
        invitationDbService.updateById(inv);
        return ms;
    }

    /**
     * 撤销邀请.
     *
     * @param id 邀请ID
     * @return true 撤销成功，false 邀请不存在或已使用/已撤销
     * @throws IllegalStateException    当邀请已被使用或已撤销时
     */
    @Override
    @Transactional
    public boolean revoke(Long id) {
        InvitationDO inv = invitationDbService.selectById(id);
        if (inv == null) { return false; }

        if (inv.getStatus() != null && inv.getStatus() != 0) {
            throw new IllegalStateException("邀请已被使用或已撤销, 不能再撤销");
        }
        inv.setStatus(3);  // 已过期 (撤销复用此状态)
        invitationDbService.updateById(inv);
        log.info("InvitationServiceImpl.revoke: id={}", id);
        return true;
    }

    /**
     * 查询租户的所有邀请.
     *
     * @param tenantCode 租户编码
     * @return 邀请列表
     */
    @Override
    public List<InvitationDO> listByTenant(String tenantCode) {
        if (tenantCode == null) { return java.util.Collections.emptyList(); }

        return invitationDbService.selectByTenant(tenantCode);
    }

    /**
     * 根据ID查询邀请.
     *
     * @param id 邀请ID
     * @return 邀请记录，不存在则返回空
     */
    @Override
    public Optional<InvitationDO> findById(Long id) {
        if (id == null) { return Optional.empty(); }

        return Optional.ofNullable(invitationDbService.selectById(id));
    }

    /**
     * 根据Token查询邀请.
     *
     * @param token 邀请 Token
     * @return 邀请记录，不存在则返回空
     */
    @Override
    public Optional<InvitationDO> findByToken(String token) {
        if (token == null) { return Optional.empty(); }

        return Optional.ofNullable(invitationDbService.selectByToken(token));
    }

    /**
     * 根据邀请码查询邀请.
     *
     * @param code 邀请码
     * @return 邀请记录，不存在则返回空
     */
    @Override
    public Optional<InvitationDO> findByInviteCode(String code) {
        if (code == null) { return Optional.empty(); }

        return Optional.ofNullable(invitationDbService.selectByInviteCode(code));
    }

    /**
     * 生成 6 位唯一邀请码，冲突时重试（最多 10 次）.
     *
     * @return 邀请码
     */
    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                sb.append(INVITE_CODE_CHARS.charAt(ThreadLocalRandom.current().nextInt(INVITE_CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (invitationDbService.selectByInviteCode(code) == null) {
                return code;
            }
        }
        // 极端情况: 重试 10 次仍冲突, 用 UUID 后 6 位
        return RandomUtil.uuidShort(6).toUpperCase();
    }
}
