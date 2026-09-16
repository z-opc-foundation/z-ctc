package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.OrgDO;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrgVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantCode;
    private String domainCode;
    private String orgCode;
    private String orgName;
    private Integer status;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;

    public static OrgVO from(OrgDO d) {
        if (d == null) { return null; }

        OrgVO v = new OrgVO();
        v.id = d.getId();
        v.tenantCode = d.getTenantCode();
        v.domainCode = d.getDomainCode();
        v.orgCode = d.getOrgCode();
        v.orgName = d.getOrgName();
        v.status = d.getStatus();
        v.description = d.getDescription();
        v.createdAt = d.getCreatedAt();
        v.createdBy = d.getCreatedBy();
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

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
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
}
