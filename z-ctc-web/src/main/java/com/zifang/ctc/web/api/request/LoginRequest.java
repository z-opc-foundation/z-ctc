package com.zifang.ctc.web.api.request;

/**
 * 登录请求.
 * <p>
 * 字段:
 * - identifier:  用户名/手机/邮箱
 * - password:    密码
 * - identityType: 1=密码
 * - tenantCode:  登录到指定租户 (优先于账号默认)
 * - domainCode:  登录到指定域 (写到 JWT claim, 不在账号主表里)
 * - source:      登录来源 (opc / ppt / 自定义) — 用于审计, 不影响逻辑
 */
public class LoginRequest {

    private String identifier;
    private String password;
    private Integer identityType;
    private String tenantCode;
    private String domainCode;
    private String source;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getIdentityType() {
        return identityType;
    }

    public void setIdentityType(Integer identityType) {
        this.identityType = identityType;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
