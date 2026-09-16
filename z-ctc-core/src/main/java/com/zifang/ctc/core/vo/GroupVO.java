package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.GroupDO;

import java.io.Serializable;
import java.time.LocalDateTime;

public class GroupVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantCode;
    private String domainCode;
    private String deptCode;
    private String groupCode;
    private String groupName;
    private Integer status;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;

    public static GroupVO from(GroupDO d) {
        if (d == null) { return null; }

        GroupVO v = new GroupVO();
        v.id = d.getId();
        v.tenantCode = d.getTenantCode();
        v.domainCode = d.getDomainCode();
        v.deptCode = d.getDeptCode();
        v.groupCode = d.getGroupCode();
        v.groupName = d.getGroupName();
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

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
