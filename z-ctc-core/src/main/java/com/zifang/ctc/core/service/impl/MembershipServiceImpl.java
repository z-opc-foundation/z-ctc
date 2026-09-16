package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.service.MembershipDbService;
import com.zifang.ctc.core.service.MembershipService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MembershipService 默认实现.
 * <p>
 * 业务语义（幂等/退出校验/状态变更） + 事务,
 * 持久化委托给 {@link MembershipDbService}.
 *
 * @author zifang
 * @since 1.0.0
 * @see MembershipService
 * @see MembershipDbService
 */
@Service
public class MembershipServiceImpl implements MembershipService {

    private static final Logger log = LogManager.getLogger(MembershipServiceImpl.class);

    private final MembershipDbService membershipDbService;

    public MembershipServiceImpl(MembershipDbService membershipDbService) {
        this.membershipDbService = membershipDbService;
    }

    /**
     * 创建用户租户关系（幂等）.
     *
     * @param userId   用户ID
     * @param tenantCode 租户编码
     * @param roleCode 角色编码（为空则默认为 "member"）
     * @param deptCode 部门编码（可选）
     * @return MembershipDO 创建或已存在的记录
     * @throws IllegalArgumentException 当 userId 或 tenantCode 为空时
     */
    @Override
    @Transactional
    public MembershipDO create(Long userId, String tenantCode, String roleCode, String deptCode) {
        if (userId == null || tenantCode == null) {
            throw new IllegalArgumentException("userId / tenantCode 必填");
        }
        // 幂等: 已存在则返回原记录
        MembershipDO exist = membershipDbService.selectByUserAndTenant(userId, tenantCode);
        if (exist != null) {
            log.info("MembershipServiceImpl.create 幂等命中: userId={}, tenant={}, id={}",
                    userId, tenantCode, exist.getId());
            return exist;
        }
        MembershipDO ms = new MembershipDO();
        ms.setUserId(userId);
        ms.setTenantCode(tenantCode);
        ms.setRoleCode(roleCode != null ? roleCode : "member");
        ms.setDeptCode(deptCode);
        ms.setMemberStatus(1);
        ms.setJoinedAt(LocalDateTime.now());
        ms.setCreatedAt(LocalDateTime.now());
        ms.setUpdatedAt(LocalDateTime.now());
        membershipDbService.insert(ms);
        log.info("MembershipServiceImpl.create 成功: userId={}, tenant={}, role={}",
                userId, tenantCode, ms.getRoleCode());
        return ms;
    }

    /**
     * 创建租户 owner（特权方法，直接调用 create）.
     *
     * @param userId     用户ID
     * @param tenantCode 租户编码
     * @return MembershipDO 创建的记录
     */
    @Override
    @Transactional
    public MembershipDO createOwner(Long userId, String tenantCode) {
        return create(userId, tenantCode, "owner", null);
    }

    /**
     * 根据用户ID和租户编码查询租户关系.
     *
     * @param userId     用户ID
     * @param tenantCode 租户编码
     * @return MembershipDO，不存在则返回空
     */
    @Override
    public Optional<MembershipDO> findByUserAndTenant(Long userId, String tenantCode) {
        if (userId == null || tenantCode == null) { return Optional.empty(); }

        return Optional.ofNullable(membershipDbService.selectByUserAndTenant(userId, tenantCode));
    }

    /**
     * 根据ID查询租户关系.
     *
     * @param id 主键ID
     * @return MembershipDO，不存在则返回空
     */
    @Override
    public Optional<MembershipDO> findById(Long id) {
        if (id == null) { return Optional.empty(); }

        return Optional.ofNullable(membershipDbService.selectById(id));
    }

    /**
     * 查询用户所有活跃的租户关系.
     *
     * @param userId 用户ID
     * @return MembershipDO 列表
     */
    @Override
    public List<MembershipDO> listActiveByUserId(Long userId) {
        return membershipDbService.selectActiveByUserId(userId);
    }

    /**
     * 查询用户所有租户关系（包含非活跃）.
     *
     * @param userId 用户ID
     * @return MembershipDO 列表
     */
    @Override
    public List<MembershipDO> listAllByUserId(Long userId) {
        return membershipDbService.selectAllByUserId(userId);
    }

    /**
     * 查询租户所有活跃的租户关系.
     *
     * @param tenantCode 租户编码
     * @return MembershipDO 列表
     */
    @Override
    public List<MembershipDO> listActiveByTenant(String tenantCode) {
        if (tenantCode == null) { return java.util.Collections.emptyList(); }

        return membershipDbService.selectActiveByTenant(tenantCode);
    }

    /**
     * 统计用户活跃的租户数量.
     *
     * @param userId 用户ID
     * @return 活跃租户数量
     */
    @Override
    public long countActiveByUserId(Long userId) {
        return membershipDbService.countActiveByUserId(userId);
    }

    /**
     * 禁用租户关系（逻辑删除）.
     *
     * @param id       主键ID
     * @param newStatus 新状态（1=启用，2=禁用，3=已退出）
     * @return true 禁用成功，false 失败
     */
    @Override
    @Transactional
    public boolean disable(Long id, int newStatus) {
        MembershipDO ms = membershipDbService.selectById(id);
        if (ms == null) { return false; }

        ms.setMemberStatus(newStatus);
        if (newStatus == 3) {
            ms.setLeftAt(LocalDateTime.now());
        }
        ms.setUpdatedAt(LocalDateTime.now());
        boolean ok = membershipDbService.updateById(ms) > 0;
        if (ok) {
            log.info("MembershipServiceImpl.disable: id={}, newStatus={}", id, newStatus);
        }
        return ok;
    }

    /**
     * 禁用租户关系（等价于 disable(id, 2)）.
     *
     * @param id 主键ID
     * @return true 禁用成功，false 失败
     */
    @Override
    @Transactional
    public boolean remove(Long id) {
        return disable(id, 2);
    }

    /**
     * 用户主动退出租户（owner 不允许退出）.
     *
     * @param id 主键ID
     * @return true 退出成功，false 失败
     * @throws IllegalStateException 当角色为 owner 时抛出
     */
    @Override
    @Transactional
    public boolean leave(Long id) {
        MembershipDO ms = membershipDbService.selectById(id);
        if (ms == null) { return false; }

        if ("owner".equals(ms.getRoleCode())) {
            throw new IllegalStateException("owner 不能退出, 需先转让 owner 身份");
        }
        return disable(id, 3);
    }
}
