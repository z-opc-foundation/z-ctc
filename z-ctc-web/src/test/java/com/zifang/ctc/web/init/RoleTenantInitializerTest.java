package com.zifang.ctc.web.init;

import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.init.TenantInitContext;
import com.zifang.ctc.core.service.AuthorizationService;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 租户角色初始化器测试 (动态代理桩 AuthorizationService, 不依赖 DB / JUnit / Spring).
 *
 * <p>验: 新租户幂等建 admin/user 两角色、创建者绑站点管理员、重跑不重复建不重复绑、
 * 已是管理员的用户不重复授。
 */
public class RoleTenantInitializerTest {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            fail++;
            System.out.println("  ✗ 测试中断: " + e);
            e.printStackTrace(System.out);
        } finally {
            System.out.println("RoleTenantInitializerTest: " + pass + " passed, " + fail + " failed");
            System.out.flush();
            System.exit(fail > 0 ? 1 : 0);
        }
    }

    private static void run() {
        StubAuth auth = new StubAuth();
        RoleTenantInitializer initializer = new RoleTenantInitializer();
        setAuth(initializer, auth.proxy());

        // ── 首次初始化 ──
        initializer.onTenantCreated(TenantInitContext.of("site-001", 1L, 100L));
        eq("建了两个角色", 2, auth.roles.size());
        eq("admin 角色在", true, auth.roles.containsKey("site-001|admin"));
        eq("user 角色在", true, auth.roles.containsKey("site-001|user"));
        RoleDO admin = auth.roles.get("site-001|admin");
        eq("admin 挂租户", "site-001", admin.getTenantCode());
        eq("创建者已绑 admin", true, auth.bindings.contains(100L + "|" + admin.getId()));

        // ── 重跑: 幂等 ──
        initializer.onTenantCreated(TenantInitContext.of("site-001", 1L, 100L));
        eq("重跑不重复建角色", 2, auth.roles.size());
        eq("重跑不重复绑定", 1, auth.bindings.size());

        // ── 换个租户: 角色按租户隔离 ──
        initializer.onTenantCreated(TenantInitContext.of("site-002", 2L, 200L));
        eq("新租户建自己的角色", 4, auth.roles.size());
        eq("新租户创建者绑定", true, auth.bindings.contains("200|" + auth.roles.get("site-002|admin").getId()));

        // ── 已是管理员的用户再初始化: 不重复授 ──
        initializer.onTenantCreated(TenantInitContext.of("site-001", 1L, 100L));
        eq("重复绑定不发生", 2, auth.bindings.size());

        // ── 无创建者 (系统建站) ──
        int before = auth.bindings.size();
        initializer.onTenantCreated(TenantInitContext.of("site-003", 3L, null));
        eq("无创建者不绑定", before, auth.bindings.size());
        eq("角色照建", true, auth.roles.containsKey("site-003|admin"));
    }

    private static void setAuth(RoleTenantInitializer initializer, AuthorizationService auth) {
        try {
            java.lang.reflect.Field f = RoleTenantInitializer.class.getDeclaredField("authorizationService");
            f.setAccessible(true);
            f.set(initializer, auth);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void eq(String name, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass++;
        } else {
            fail++;
            System.out.println("  ✗ " + name + ": expected=" + expected + " actual=" + actual);
        }
    }

    /** AuthorizationService 桩: 只挂角色四方法, 其余不可用 */
    static class StubAuth {
        final Map<String, RoleDO> roles = new LinkedHashMap<>();
        final List<String> bindings = new ArrayList<>();
        long seq = 0;

        AuthorizationService proxy() {
            return (AuthorizationService) Proxy.newProxyInstance(
                    AuthorizationService.class.getClassLoader(),
                    new Class<?>[]{AuthorizationService.class},
                    (p, method, margs) -> {
                        switch (method.getName()) {
                            case "findRoleByCode": {
                                String roleCode = (String) margs[0];
                                String tenantCode = (String) margs[1];
                                RoleDO r = tenantCode == null ? null : roles.get(tenantCode + "|" + roleCode);
                                return Optional.ofNullable(r);
                            }
                            case "createRole": {
                                RoleDO r = (RoleDO) margs[0];
                                r.setId(++seq);
                                roles.put(r.getTenantCode() + "|" + r.getRoleCode(), r);
                                return r.getId();
                            }
                            case "grantUserRole": {
                                bindings.add(margs[0] + "|" + margs[1]);
                                return Boolean.TRUE;
                            }
                            case "listUserRoles": {
                                Long userId = (Long) margs[0];
                                List<RoleDO> out = new ArrayList<>();
                                for (String b : bindings) {
                                    String[] parts = b.split("\\|");
                                    if (String.valueOf(userId).equals(parts[0])) {
                                        for (RoleDO r : roles.values()) {
                                            if (String.valueOf(r.getId()).equals(parts[1])) {
                                                out.add(r);
                                            }
                                        }
                                    }
                                }
                                return out;
                            }
                            case "toString":
                                return "StubAuth";
                            case "hashCode":
                                return System.identityHashCode(p);
                            case "equals":
                                return p == margs[0];
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }
    }
}
