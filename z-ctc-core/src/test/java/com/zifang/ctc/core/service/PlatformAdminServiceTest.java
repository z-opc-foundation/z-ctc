package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.domain.entity.UserRoleDO;
import com.zifang.ctc.core.domain.service.AuthorizationDbService;
import com.zifang.ctc.core.service.PlatformAdminService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 平台管理员判定守卫测试（内存桩，不依赖 DB / JUnit，与 AcDeptTreeServiceTest 同风格）.
 * <p>
 * 口径: 持有系统管理员角色 (role_code ∈ {SUPER_ADMIN, PLATFORM_ADMIN}, 角色 status=1),
 * 通过 z_ctc_spc_user_role 绑定 (未过期) ⇒ 平台管理员. 判定只看编码, 不看角色挂在哪个租户下.
 */
public class PlatformAdminServiceTest {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        // ── 1. 持有 SUPER_ADMIN ⇒ 平台管理员 ──
        eq("SUPER_ADMIN", true, svc(
                bindings(1, 10),
                roles(role(10, "SUPER_ADMIN", null, 1))).isPlatformAdmin(1L));

        // ── 2. 持有 PLATFORM_ADMIN ⇒ 平台管理员 ──
        eq("PLATFORM_ADMIN", true, svc(
                bindings(2, 11),
                roles(role(11, "PLATFORM_ADMIN", null, 1))).isPlatformAdmin(2L));

        // ── 3. 角色行挂在具体租户下 ⇒ 仍算 (内置初始化在 default 下的那条 SUPER_ADMIN 必须生效) ──
        eq("租户下的 SUPER_ADMIN 同样生效", true, svc(
                bindings(3, 12),
                roles(role(12, "SUPER_ADMIN", "default", 1))).isPlatformAdmin(3L));

        // ── 4. 全局普通角色 ⇒ 不是 ──
        eq("全局 admin/member 不算", false, svc(
                bindings(4, 13, 14),
                roles(role(13, "admin", null, 1), role(14, "member", null, 1))).isPlatformAdmin(4L));

        // ── 5. 角色被禁用 ⇒ 不是 ──
        eq("禁用角色不算", false, svc(
                bindings(5, 15),
                roles(role(15, "SUPER_ADMIN", null, 0))).isPlatformAdmin(5L));

        // ── 6. 无绑定 ⇒ 不是 ──
        eq("无角色绑定", false, svc(
                Collections.emptyList(),
                Collections.emptyList()).isPlatformAdmin(6L));

        // ── 7. userId null ⇒ 不是 ──
        eq("userId null", false, svc(
                Collections.emptyList(),
                Collections.emptyList()).isPlatformAdmin(null));

        // ── 8. 判定链路异常 ⇒ 安全侧倾斜, 不是 ──
        AuthorizationDbService broken = (AuthorizationDbService) java.lang.reflect.Proxy.newProxyInstance(
                AuthorizationDbService.class.getClassLoader(),
                new Class<?>[]{AuthorizationDbService.class},
                (proxy, method, margs) -> {
                    throw new RuntimeException("db down");
                });
        eq("异常降级为拒绝", false, new PlatformAdminServiceImpl(broken).isPlatformAdmin(7L));

        // ── 9. 角色编码为空 ⇒ 不是 (不抛 NPE) ──
        eq("roleCode 空值不崩", false, svc(
                bindings(8, 16),
                roles(role(16, null, null, 1))).isPlatformAdmin(8L));

        System.out.println("---- PlatformAdminServiceTest ----");
        System.out.println("pass=" + pass + ", fail=" + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }

    // ===== 桩 =====

    /** 只拦截 selectUserRoleByUserId / selectRoleBatchIds, 其余方法抛 UnsupportedOperationException. */
    private static AuthorizationDbService stub(List<UserRoleDO> bindings, List<RoleDO> roles) {
        return (AuthorizationDbService) java.lang.reflect.Proxy.newProxyInstance(
                AuthorizationDbService.class.getClassLoader(),
                new Class<?>[]{AuthorizationDbService.class},
                (proxy, method, margs) -> {
                    switch (method.getName()) {
                        case "selectUserRoleByUserId":
                            return bindings;
                        case "selectRoleBatchIds":
                            return roles;
                        case "toString":
                            return "AuthorizationDbServiceStub";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == margs[0];
                        default:
                            throw new UnsupportedOperationException("stub 未实现: " + method.getName());
                    }
                });
    }

    private static PlatformAdminService svc(List<UserRoleDO> bindings, List<RoleDO> roles) {
        return new PlatformAdminServiceImpl(stub(bindings, roles));
    }

    private static List<UserRoleDO> bindings(int userId, long... roleIds) {
        java.util.List<UserRoleDO> list = new java.util.ArrayList<>();
        for (long rid : roleIds) {
            UserRoleDO ur = new UserRoleDO();
            ur.setUserId((long) userId);
            ur.setRoleId(rid);
            list.add(ur);
        }
        return list;
    }

    private static List<RoleDO> roles(RoleDO... rs) {
        return Arrays.asList(rs);
    }

    private static RoleDO role(long id, String code, String tenantCode, int status) {
        RoleDO r = new RoleDO();
        r.setId(id);
        r.setRoleCode(code);
        r.setTenantCode(tenantCode);
        r.setStatus(status);
        return r;
    }

    private static void eq(String name, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass++;
            System.out.println("PASS " + name);
        } else {
            fail++;
            System.out.println("FAIL " + name + " expected=" + expected + " actual=" + actual);
        }
    }
}
