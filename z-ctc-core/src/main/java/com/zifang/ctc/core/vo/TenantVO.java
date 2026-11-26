package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.TenantDO;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户 VO — API 响应模型, 不暴露 DO 注解和持久化细节.
 */
public class TenantVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantCode;
    private String tenantName;
    private Integer status;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;

    public static TenantVO from(TenantDO d) {
        if (d == null) { return null; }

        TenantVO v = new TenantVO();
        v.id = d.getId();
        v.tenantCode = d.getTenantCode();
        v.tenantName = d.getTenantName();
        v.status = d.getStatus();
        v.contactName = d.getContactName();
        v.contactPhone = d.getContactPhone();
        v.contactEmail = d.getContactEmail();
        v.expireAt = d.getExpireAt();
        v.createdAt = d.getCreatedAt();
        v.createdBy = d.getCreatedBy();
        v.updatedAt = d.getUpdatedAt();
        return v;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
