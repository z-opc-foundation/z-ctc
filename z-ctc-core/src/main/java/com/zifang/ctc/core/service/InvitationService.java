package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.InvitationDO;
import com.zifang.ctc.core.domain.entity.MembershipDO;

import java.util.List;
import java.util.Optional;

/**
 * 邀请服务 (FEATURE049).
 * <p>
 * 邀请方式:
 * - 邀请链接: 带 token (UUID), 精准定向
 * - 邀请码: 短码 (6-8位), 手动输入, max_uses > 1 时可批量邀请
 */
public interface InvitationService {

    /**
     * 创建邀请.
     *
     * @param tenantCode    目标租户
     * @param inviterUserId 邀请人 user_id
     * @param inviteeEmail  被邀请人邮箱 (链接场景)
     * @param inviteePhone  被邀请人手机号 (备用)
     * @param roleCode      接受后授予的角色
     * @param maxUses       邀请码最大使用次数 (链接场景 1, 码场景可 >1)
     * @param type          "link" (生成 token) / "code" (生成 6 位邀请码)
     * @return 新建 invitation
     */
    InvitationDO create(String tenantCode, Long inviterUserId, String inviteeEmail,
                        String inviteePhone, String roleCode, Integer maxUses, String type);

    /**
     * 接受邀请 (链接 token).
     * 校验 token 有效 + 未过期 + 未用完, 然后:
     * - 新建 user (若 inviteeEmail 不存在)
     * - 建立 membership
     * - 标记 invitation 状态为已接受
     *
     * @return 新建/已存在的 membership
     */
    MembershipDO acceptByToken(String token, Long acceptorUserId);

    /**
     * 兑换邀请码.
     * 校验 code 有效 + 未过期 + used_count < max_uses, 然后同上.
     */
    MembershipDO redeemByCode(String code, Long acceptorUserId);

    /**
     * 撤销邀请 (owner/admin).
     * invitation.status = 3 (已过期).
     */
    boolean revoke(Long id);

    List<InvitationDO> listByTenant(String tenantCode);

    Optional<InvitationDO> findById(Long id);

    Optional<InvitationDO> findByToken(String token);

    Optional<InvitationDO> findByInviteCode(String code);
}
