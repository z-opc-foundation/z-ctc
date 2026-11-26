package com.zifang.ctc.sso.hook;

/**
 * CTC 租户上下文钩子.
 *
 * <p>z-ctc-sso 定义接口, 调用方通过此钩子获取当前租户信息.
 * 支持多种实现方式 (HTTP / RPC / 本地).
 *
 * <p>使用方式:
 * <pre>{@code
 * TenantContext ctx = ctcTenantHook.getCurrentTenant();
 * String tenantCode = ctx.getTenantCode();
 * }</pre>
 */
public interface CtcTenantHook {

    /**
     * 获取当前租户上下文.
     *
     * @return 租户上下文, 未登录返回 null
     */
    TenantContext getCurrentTenant();

    /**
     * 钩子类型标识.
     */
    default String hookType() {
        return "ctc-tenant";
    }

    /**
     * 租户上下文模型.
     */
    class TenantContext {
        private final String tenantCode;
        private final String domainCode;
        private final String userId;

        public TenantContext(String tenantCode, String domainCode, String userId) {
            this.tenantCode = tenantCode;
            this.domainCode = domainCode;
            this.userId = userId;
        }

        public String getTenantCode() { return tenantCode; }
        public String getDomainCode() { return domainCode; }
        public String getUserId() { return userId; }
    }
}
