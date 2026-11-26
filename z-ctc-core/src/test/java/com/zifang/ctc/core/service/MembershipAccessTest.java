package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.service.MembershipDbService;
import com.zifang.ctc.core.service.impl.MembershipServiceImpl;

/**
 * 租户内访问边界测试（内存桩，不依赖 DB / JUnit，与 PlatformAdminServiceTest 同风格）.
 * <p>
 * 口径: isMember = 活跃成员 (member_status=1);
 * isTenantAdmin = 活跃成员 且 role_code ∈ {owner, admin}.
 */
public class MembershipAccessTest {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        // ── 1. owner 活跃 ⇒ 可管 ──
        eq("owner 可管", true, svc(membership("owner", 1)).isTenantAdmin(1L, "t1"));

        // ── 2. admin 活跃 ⇒ 可管 ──
        eq("admin 可管", true, svc(membership("admin", 1)).isTenantAdmin(1L, "t1"));

        // ── 3. member 活跃 ⇒ 能看不能管 ──
        eq("member 能看", true, svc(membership("member", 1)).isMember(1L, "t1"));
        eq("member 不能管", false, svc(membership("member", 1)).isTenantAdmin(1L, "t1"));

        // ── 4. 已禁用/已退出 ⇒ 既不能看也不能管 ──
        eq("禁用成员不能看", false, svc(membership("owner", 2)).isMember(1L, "t1"));
        eq("禁用成员不能管", false, svc(membership("owner", 2)).isTenantAdmin(1L, "t1"));

        // ── 5. 无关系 ⇒ 都不行 ──
        eq("无关系不能看", false, svc(null).isMember(1L, "t1"));
        eq("无关系不能管", false, svc(null).isTenantAdmin(1L, "t1"));

        // ── 6. 参数缺失 ⇒ 都不行 (不抛 NPE) ──
        eq("userId null", false, svc(membership("owner", 1)).isMember(null, "t1"));
        eq("tenantCode 空", false, svc(membership("owner", 1)).isTenantAdmin(1L, ""));

        System.out.println("---- MembershipAccessTest ----");
        System.out.println("pass=" + pass + ", fail=" + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }

    // ===== 桩 =====

    /** 只拦截 selectByUserAndTenant, 其余方法抛 UnsupportedOperationException. */
    private static MembershipDbService stub(MembershipDO row) {
        return (MembershipDbService) java.lang.reflect.Proxy.newProxyInstance(
                MembershipDbService.class.getClassLoader(),
                new Class<?>[]{MembershipDbService.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "selectByUserAndTenant":
                            return row;
                        case "toString":
                            return "MembershipDbServiceStub";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            throw new UnsupportedOperationException("stub 未实现: " + method.getName());
                    }
                });
    }

    private static MembershipService svc(MembershipDO row) {
        return new MembershipServiceImpl(stub(row));
    }

    private static MembershipDO membership(String roleCode, int memberStatus) {
        MembershipDO m = new MembershipDO();
        m.setUserId(1L);
        m.setTenantCode("t1");
        m.setRoleCode(roleCode);
        m.setMemberStatus(memberStatus);
        return m;
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
