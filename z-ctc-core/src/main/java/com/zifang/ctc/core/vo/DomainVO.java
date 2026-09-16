package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.DomainDO;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DomainVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String domainCode;
    private String domainName;
    private String tenantCode;
    private Integer status;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;

    public static DomainVO from(DomainDO d) {
        if (d == null) { return null; }

        DomainVO v = new DomainVO();
        v.id = d.getId();
        v.domainCode = d.getDomainCode();
        v.domainName = d.getDomainName();
        v.tenantCode = d.getTenantCode();
        v.status = d.getStatus();
        v.description = d.getDescription();
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

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
