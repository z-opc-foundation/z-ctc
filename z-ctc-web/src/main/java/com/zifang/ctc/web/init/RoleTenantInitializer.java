package com.zifang.ctc.web.init;

import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.init.TenantInitContext;
import com.zifang.ctc.core.init.TenantInitializer;
import com.zifang.ctc.core.service.AuthorizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 租户角色初始化器 (建站 T0 身份地基的「角色」一块).
 *
 * <p>新建租户后自动:
 * <ul>
 *   <li>幂等建两个租户级角色: {@code admin}(站点管理员) 与 {@code user}(普通用户)</li>
 *   <li>创建者 (ctx.createdBy) 绑定为站点管理员 —— 建站的人天然是该站点的第一个管理员</li>
 * </ul>
 *
 * <p>order=20: 排在 CtcTenantInitializer (默认域/组织, order=10) 之后.
 * 幂等: 角色按 (roleCode, tenantCode) 查重; 绑定先查 listUserRoles 再授, 不产生重复行.
 *
 * <p>异常处理同 CtcTenantInitializer: 包装成 InitFailedException 抛给 Orchestrator 报告, 不吞.
 */
@Component
public class RoleTenantInitializer implements TenantInitializer {

    private static final Logger log = LogManager.getLogger(RoleTenantInitializer.class);

    /** 站点管理员角色编码 */
    public static final String ROLE_ADMIN = "admin";

    /** 普通用户角色编码 */
    public static final String ROLE_USER = "user";

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public String moduleName() {
        return "z-ctc";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void onTenantCreated(TenantInitContext ctx) {
        String tenantCode = ctx.tenantCode();
        String createdBy = ctx.createdBy() == null ? null : String.valueOf(ctx.createdBy());

        Long adminRoleId = ensureRole(tenantCode, ROLE_ADMIN, "站点管理员",
                "站点管理员: 管理本站点的账号、组织与全部资源", createdBy);
        ensureRole(tenantCode, ROLE_USER, "普通用户",
                "普通用户: 使用本站点的业务功能", createdBy);

        // 创建者 → 站点管理员 (幂等: 已绑不重复授)
        if (ctx.createdBy() != null) {
            ensureAdminBinding(ctx.createdBy(), tenantCode, adminRoleId);
        }
        log.info("[z-ctc-init] 租户 {} 角色初始化完成 (admin/user + 创建者绑定)", tenantCode);
    }

    /** 幂等创建租户级角色, 返回角色 id. 失败抛 InitFailedException 让 Orchestrator 报告. */
    private Long ensureRole(String tenantCode, String roleCode, String roleName, String description, String createdBy) {
        try {
            Optional<RoleDO> existing = authorizationService.findRoleByCode(roleCode, tenantCode);
            if (existing.isPresent()) {
                return existing.get().getId();
            }
            RoleDO role = new RoleDO();
            role.setRoleCode(roleCode);
            role.setRoleName(roleName);
            role.setTenantCode(tenantCode);
            role.setDescription(description);
            role.setStatus(1);
            Long id = authorizationService.createRole(role, createdBy == null ? "site-init" : createdBy);
            log.info("[z-ctc-init] 租户 {} 创建角色 {} id={}", tenantCode, roleCode, id);
            return id;
        } catch (Exception e) {
            throw new CtcTenantInitializer.InitFailedException(
                    "建角色 " + roleCode + " 失败: " + e.getMessage(), e);
        }
    }

    private void ensureAdminBinding(Long userId, String tenantCode, Long adminRoleId) {
        if (adminRoleId == null) {
            throw new CtcTenantInitializer.InitFailedException("创建者绑定失败: admin 角色 id 为空", null);
        }
        try {
            List<RoleDO> owned = authorizationService.listUserRoles(userId);
            for (RoleDO r : owned) {
                if (r != null && ROLE_ADMIN.equals(r.getRoleCode()) && tenantCode.equals(r.getTenantCode())) {
                    log.debug("[z-ctc-init] 用户 {} 已是租户 {} 管理员, 跳过绑定", userId, tenantCode);
                    return;
                }
            }
            authorizationService.grantUserRole(userId, adminRoleId, "site-init", null);
            log.info("[z-ctc-init] 用户 {} 已绑定为租户 {} 站点管理员", userId, tenantCode);
        } catch (Exception e) {
            throw new CtcTenantInitializer.InitFailedException(
                    "创建者绑定为站点管理员失败: " + e.getMessage(), e);
        }
    }
}
