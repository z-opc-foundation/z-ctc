package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@BizModel(name = "z_ctc_spc_resource", module = "z-ctc", description = "z_ctc_spc_resource")
@TableName("z_ctc_spc_resource")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class ResourceDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("resource_code")
    @BizField(name = "资源编码 (业务唯一, 如 user:create / order:list)", type = BizField.FieldType.STRING)
    private String resourceCode;

    @TableField("resource_name")
    @BizField(name = "资源名", type = BizField.FieldType.STRING)
    private String resourceName;

    @TableField("resource_type")
    @BizField(name = "资源类型: 1=菜单/功能 2=数据权限 3=API 接口 4=按钮", type = BizField.FieldType.INTEGER)
    private Integer resourceType;

    /**
     * FEATURE015: 所属应用编码（NULL=公共/跨应用）
     */
    @TableField("app_code")
    @BizField(name = "所属应用编码（NULL=公共/跨应用）— FEATURE015 新增", type = BizField.FieldType.STRING)
    private String appCode;

    @TableField("parent_id")
    @BizField(name = "父资源 ID (菜单树)", type = BizField.FieldType.INTEGER)
    private Long parentId;

    @TableField("path")
    @BizField(name = "路径 (菜单/接口 URL)", type = BizField.FieldType.STRING)
    private String path;

    @TableField("method")
    @BizField(name = "HTTP 方法 (GET/POST/...) — 接口资源用", type = BizField.FieldType.STRING)
    private String method;

    @TableField("icon")
    @BizField(name = "图标 (菜单用)", type = BizField.FieldType.STRING)
    private String icon;

    @TableField("sort_order")
    @BizField(name = "排序", type = BizField.FieldType.INTEGER)
    private Integer sortOrder;

    @TableField("tenant_code")
    @BizField(name = "租户", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("description")
    @BizField(name = "描述", type = BizField.FieldType.STRING)
    private String description;

    @TableField("status")
    @BizField(name = "状态: 0=禁用 1=正常", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("created_at")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdAt;

    @TableField("created_by")
    @BizField(name = "创建人", type = BizField.FieldType.STRING)
    private String createdBy;

    @TableField("updated_at")
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedAt;

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



