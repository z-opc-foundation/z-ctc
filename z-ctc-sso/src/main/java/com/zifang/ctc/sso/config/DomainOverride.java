package com.zifang.ctc.sso.config;

/**
 * 域名 → 租户/域 覆盖规则.
 * <p>
 * 用途: 不同的子域名强制登录到不同的租户/域. 例如:
 * - ppt.zopc.top → tenant=default, domain=tool_domain
 * - opc.zopc.top → tenant=default, domain=opc_domain
 * <p>
 * 匹配规则: 完全匹配 host (大小写不敏感). 也支持通配符模式:
 * - "*.zopc.top" → 匹配所有 zopc.top 子域名
 * - "ppt.zopc.top" → 仅匹配 ppt.zopc.top
 * <p>
 * 配置 (application.yml):
 * sso:
 * domain-overrides:
 * - host: ppt.zopc.top
 * tenantCode: default
 * domainCode: tool_domain
 * - host: '*.zopc.top'
 * tenantCode: default
 * domainCode: opc_domain
 */
public class DomainOverride {

    private String host;
    private String tenantCode;
    private String domainCode;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public boolean match(String hostname) {
        if (host == null || hostname == null) { return false; }

        String h = hostname.toLowerCase();
        String p = host.toLowerCase();
        if (p.startsWith("*.")) {
            // 通配符: *.zopc.top 匹配 a.zopc.top / b.zopc.top
            String suffix = p.substring(1); // ".zopc.top"
            return h.endsWith(suffix);
        }
        return h.equals(p);
    }
}
