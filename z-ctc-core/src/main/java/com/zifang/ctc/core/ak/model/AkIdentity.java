package com.zifang.ctc.core.ak.model;

/** 验签结果：只含身份和范围，绝不携带 secret 或 hash。 */
public class AkIdentity {
    private String akId;
    private String akCode;
    private String akMasked;
    private String tenantCode;
    private String akName;
    private String ownerType;
    private String ownerId;
    private String ownerName;
    private boolean enabled;

    public String getAkId() { return akId; }
    public void setAkId(String akId) { this.akId = akId; }
    public String getAkCode() { return akCode; }
    public void setAkCode(String akCode) { this.akCode = akCode; }
    public String getAkMasked() { return akMasked; }
    public void setAkMasked(String akMasked) { this.akMasked = akMasked; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getAkName() { return akName; }
    public void setAkName(String akName) { this.akName = akName; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
