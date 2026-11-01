package com.zifang.ctc.core.service;

/**
 * 平台管理员判定服务 (平台级租户管理面板).
 * <p>
 * 口径: 用户在全局角色表 (z_ctc_spc_role.tenant_code IS NULL, 即公共角色) 中
 * 持有 role_code ∈ {SUPER_ADMIN, PLATFORM_ADMIN} 的角色绑定 (z_ctc_spc_user_role,
 * 未过期), 即视为平台管理员 — 可跨租户管理所有租户.
 * <p>
 * 与租户内管理员 (membership.roleCode = owner/admin) 是两个层面:
 * 租户内管理员只能管自己租户, 平台管理员管所有租户.
 */
public interface PlatformAdminService {

    /**
     * 判定用户是否平台管理员.
     *
     * @param userId 用户主键 (z_ctc_ac_user.id / account id)
     * @return true = 平台管理员
     */
    boolean isPlatformAdmin(Long userId);
}
