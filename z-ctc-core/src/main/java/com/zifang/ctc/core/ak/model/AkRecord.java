package com.zifang.ctc.core.ak.model;

/** AK 台账行；create 响应额外携带一次性的 secret。 */
public class AkRecord {

    private String akId;
    private String tenantCode;
    private String akCode;
    private String akName;
    /** SYSTEM / PERSONAL / APPLICATION。 */
    private String ownerType;
    private String ownerId;
    private String ownerName;
    private java.time.Instant createTime;
    /** 仅 create 响应返回一次；list 恒为空。 */
    private String secret;
    private Integer status;
    private java.util.Map<String, java.util.List<com.zifang.ctc.core.ak.model.AkScope>> scopes;

    public String getAkId() {
        return akId;
    }

    public void setAkId(String akId) {
        this.akId = akId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getAkCode() {
        return akCode;
    }

    public void setAkCode(String akCode) {
        this.akCode = akCode;
    }

    public String getAkName() {
        return akName;
    }

    public void setAkName(String akName) {
        this.akName = akName;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public java.time.Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.time.Instant createTime) {
        this.createTime = createTime;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public java.util.Map<String, java.util.List<com.zifang.ctc.core.ak.model.AkScope>> getScopes() {
        return scopes;
    }

    public void setScopes(
            java.util.Map<String, java.util.List<com.zifang.ctc.core.ak.model.AkScope>> scopes) {
        this.scopes = scopes;
    }
}