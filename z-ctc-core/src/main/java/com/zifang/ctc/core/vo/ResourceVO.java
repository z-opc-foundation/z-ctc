package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.ResourceDO;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资源 VO.
 */
public class ResourceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String resourceCode;
    private String resourceName;
    private Integer resourceType;
    private String appCode;
    private Long parentId;
    private String path;
    private String method;
    private String icon;
    private Integer sortOrder;
    private String tenantCode;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;

    public ResourceVO() {
    }

    public static ResourceVO from(ResourceDO resourceDO) {
        if (resourceDO == null) {
            return null;
        }
        ResourceVO vo = new ResourceVO();
        vo.setId(resourceDO.getId());
        vo.setResourceCode(resourceDO.getResourceCode());
        vo.setResourceName(resourceDO.getResourceName());
        vo.setResourceType(resourceDO.getResourceType());
        vo.setAppCode(resourceDO.getAppCode());
        vo.setParentId(resourceDO.getParentId());
        vo.setPath(resourceDO.getPath());
        vo.setMethod(resourceDO.getMethod());
        vo.setIcon(resourceDO.getIcon());
        vo.setSortOrder(resourceDO.getSortOrder());
        vo.setTenantCode(resourceDO.getTenantCode());
        vo.setDescription(resourceDO.getDescription());
        vo.setStatus(resourceDO.getStatus());
        vo.setCreatedAt(resourceDO.getCreatedAt());
        vo.setCreatedBy(resourceDO.getCreatedBy());
        vo.setUpdatedAt(resourceDO.getUpdatedAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getResourceType() {
        return resourceType;
    }

    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}