package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.domain.entity.UserRoleDO;
import com.zifang.ctc.core.domain.service.AuthorizationDbService;
import com.zifang.ctc.core.service.PlatformAdminService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 平台管理员判定默认实现.
 * <p>
 * 口径一条: 持有系统管理员角色 (role_code ∈ {SUPER_ADMIN, PLATFORM_ADMIN} 且角色已启用)
 * 的未过期绑定 ⇒ 平台管理员.
 * <p>
 * 判定只看角色编码, 不看角色的 tenant_code: 这两个是系统内置保留角色, 语义就是
 * "持有人可跨租户管理", 角色行落在哪个租户下不改变这个语义 (内置初始化脚本给的是
 * tenant_code = NULL, 历史库里有一条挂在 default 下, 两者都应生效).
 * 每次实时查库 (不缓存到 JWT), 角色回收后立即失效.
 */
@Service
public class PlatformAdminServiceImpl implements PlatformAdminService {

    private static final Logger log = LogManager.getLogger(PlatformAdminServiceImpl.class);

    /** 系统管理员角色编码 (内置保留). */
    private static final Set<String> PLATFORM_ADMIN_ROLE_CODES =
            new HashSet<>(Arrays.asList("SUPER_ADMIN", "PLATFORM_ADMIN"));

    private static final Integer ROLE_ENABLED = 1;

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
            List<UserRoleDO> bindings = authzDbService.selectUserRoleByUserId(userId);
            if (bindings.isEmpty()) {
                return false;
            }
            List<Long> roleIds = bindings.stream().map(UserRoleDO::getRoleId).collect(Collectors.toList());
            return authzDbService.selectRoleBatchIds(roleIds).stream().anyMatch(this::isAdminRole);
        } catch (Exception e) {
            // 判定失败按无权限处理 (安全侧倾斜), 不阻断请求
            log.warn("PlatformAdminServiceImpl.isPlatformAdmin 判定异常, 按非平台管理员处理: userId={}, err={}",
                    userId, e.getMessage());
            return false;
        }
    }

    /** 系统管理员角色 = 编码命中且角色未停用. */
    private boolean isAdminRole(RoleDO role) {
        return role != null
                && ROLE_ENABLED.equals(role.getStatus())
                && PLATFORM_ADMIN_ROLE_CODES.contains(role.getRoleCode());
    }
}
