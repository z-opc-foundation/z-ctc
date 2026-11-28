package com.zifang.ctc.core.service.session;

/** 机器 / 服务账号会话视图（不含令牌哈希）。 */
public class ServiceSession {

    private String id;
    private String tenantCode;
    private String subjectType;
    private String subjectCode;
    private long issuedAt;
    private long expiresAt;
    private long lastSeenAt;
    /** 仅签发响应携带一次；验签响应恒为空。 */
    private String token;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public long getIssuedAt() { return issuedAt; }
    public void setIssuedAt(long issuedAt) { this.issuedAt = issuedAt; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public long getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(long lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
