package com.zifang.ctc.web.api.request;

/**
 * 切换租户 / 域 请求.
 * <p>
 * FEATURE012 增量: 切租户后重新签发 JWT, 携带新的 tenantCode/domainCode claim.
 * 与 LoginRequest 分离 — 切换不需要传 identifier/password, 身份从当前 JWT 中取.
 */
public class SwitchTenantRequest {

    private String tenantCode;
    private String domainCode;

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
}
