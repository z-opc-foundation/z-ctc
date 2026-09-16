package com.zifang.ctc.core.tenant;

/**
 * 租户上下文 (FEATURE049).
 * <p>
 * ThreadLocal 持有当前请求的租户身份, 由 {@link TenantContextFilter} 在请求开始时写入, 结束时清理.
 * <p>
 * 业务层 (MyBatis 拦截器 / 业务服务) 可通过 {@link #get()} 拿到当前租户编码,
 * 用于自动注入 tenant_code / 权限检查.
 *
 * <h3>使用范式</h3>
 * <pre>
 *   // 业务代码
 *   String tenant = TenantContext.getTenantCode();
 *   if (tenant == null) {
 *       // 游离态, 不能访问业务数据
 *   }
 *
 *   // 过滤条件自动追加
 *   qw.eq("tenant_code", TenantContext.getTenantCode());
 * </pre>
 */
public final class TenantContext {

    private static final ThreadLocal<TenantInfo> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * 在请求处理开始时设置租户上下文 (Filter 调用).
     */
    public static void set(TenantInfo info) {
        HOLDER.set(info);
    }

    /**
     * 拿到完整的租户信息 (可能为 null — 表示游离态或未登录).
     */
    public static TenantInfo get() {
        return HOLDER.get();
    }

    /**
     * 拿到当前 user id (无则 null).
     */
    public static Long getUserId() {
        TenantInfo info = HOLDER.get();
        return info == null ? null : info.getUserId();
    }

    /**
     * 拿到当前 tenant code (无则 null — 游离态).
     */
    public static String getTenantCode() {
        TenantInfo info = HOLDER.get();
        return info == null ? null : info.getTenantCode();
    }

    /**
     * 拿到当前 domain code (无则 null).
     */
    public static String getDomainCode() {
        TenantInfo info = HOLDER.get();
        return info == null ? null : info.getDomainCode();
    }

    /**
     * 拿到当前 role code (无则 null).
     */
    public static String getRoleCode() {
        TenantInfo info = HOLDER.get();
        return info == null ? null : info.getRoleCode();
    }

    /**
     * 是否游离态 (已登录但 0 个 active membership).
     */
    public static boolean isOrphan() {
        TenantInfo info = HOLDER.get();
        if (info == null || info.getUserId() == null) return false;

        return info.getTenantCode() == null || info.getTenantCode().isEmpty();
    }

    /**
     * 清理 ThreadLocal (Filter finally 块调用, 避免线程复用泄露).
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 不可变值对象: 当前请求的租户身份.
     */
    public static class TenantInfo {
        private final Long userId;
        private final String tenantCode;
        private final String domainCode;
        private final String roleCode;

        public TenantInfo(Long userId, String tenantCode, String domainCode, String roleCode) {
            this.userId = userId;
            this.tenantCode = tenantCode;
            this.domainCode = domainCode;
            this.roleCode = roleCode;
        }

        public static TenantInfo of(Long userId, String tenantCode, String domainCode, String roleCode) {
            return new TenantInfo(userId, tenantCode, domainCode, roleCode);
        }

        public Long getUserId() {
            return userId;
        }

        public String getTenantCode() {
            return tenantCode;
        }

        public String getDomainCode() {
            return domainCode;
        }

        public String getRoleCode() {
            return roleCode;
        }
    }
}
