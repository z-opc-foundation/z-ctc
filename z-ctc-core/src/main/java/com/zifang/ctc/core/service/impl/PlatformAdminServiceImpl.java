package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.domain.entity.UserRoleDO;
import com.zifang.ctc.core.domain.service.AuthorizationDbService;
import com.zifang.ctc.core.service.PlatformAdminService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 平台管理员判定默认实现.
 * <p>
 * 复用现有 RBAC 表 (z_ctc_spc_role + z_ctc_spc_user_role), 不新造表:
 * <ol>
 *   <li>查全局公共角色 (tenant_code IS NULL, status=1) 中 role_code ∈ {SUPER_ADMIN, PLATFORM_ADMIN} 的角色</li>
 *   <li>再查用户是否持有其中任一角色 (selectUserRoleByUserId 已带 expire_at 过滤)</li>
 * </ol>
 * 每次实时查库 (不缓存到 JWT), 保证回收角色后立即生效.
 */
@Service
public class PlatformAdminServiceImpl implements PlatformAdminService {

    private static final Logger log = LogManager.getLogger(PlatformAdminServiceImpl.class);

    /** 平台管理员角色编码 (公共全局角色). */
    private static final Set<String> PLATFORM_ADMIN_ROLE_CODES =
            new java.util.HashSet<>(Arrays.asList("SUPER_ADMIN", "PLATFORM_ADMIN"));

    private final AuthorizationDbService authzDbService;

    public PlatformAdminServiceImpl(AuthorizationDbService authzDbService) {
        this.authzDbService = authzDbService;
    }

    @Override
    public boolean isPlatformAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            // 1. 用户的全部有效角色绑定 (未过期)
            List<UserRoleDO> urs = authzDbService.selectUserRoleByUserId(userId);
            if (urs.isEmpty()) {
                return false;
            }
            // 2. 解析角色, 命中"全局公共平台管理员角色"即判定通过
            List<Long> roleIds = urs.stream().map(UserRoleDO::getRoleId).collect(Collectors.toList());
            for (RoleDO role : authzDbService.selectRoleBatchIds(roleIds)) {
                if (role == null || role.getRoleCode() == null) {
                    continue;
                }
                boolean globalPublicRole = role.getTenantCode() == null || role.getTenantCode().isEmpty();
                if (globalPublicRole && role.getStatus() != null && role.getStatus() == 1
                        && PLATFORM_ADMIN_ROLE_CODES.contains(role.getRoleCode())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            // 判定失败按无权限处理 (安全侧倾斜), 不阻断请求
            log.warn("PlatformAdminServiceImpl.isPlatformAdmin 判定异常, 按非平台管理员处理: userId={}, err={}",
                    userId, e.getMessage());
            return false;
        }
    }
}
