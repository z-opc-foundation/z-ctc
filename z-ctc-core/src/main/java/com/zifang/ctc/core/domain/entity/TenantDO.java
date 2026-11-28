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

/**
 * 租户实体.
 */
@BizModel(name = "租户实体", module = "z-ctc", description = "租户实体.")
@TableName("z_ctc_ac_tenant")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class TenantDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("tenant_code")
    @BizField(name = "租户编码 (业务唯一标识)", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("tenant_name")
    @BizField(name = "租户名", type = BizField.FieldType.STRING)
    private String tenantName;

    @TableField("status")
    @BizField(name = "状态: 0=禁用 1=正常", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("contact_name")
    @BizField(name = "联系人", type = BizField.FieldType.STRING)
    private String contactName;

    @TableField("contact_phone")
    @BizField(name = "联系电话", type = BizField.FieldType.STRING)
    private String contactPhone;

    @TableField("contact_email")
    @BizField(name = "联系邮箱", type = BizField.FieldType.STRING)
    private String contactEmail;

    @TableField("expire_at")
    @BizField(name = "租户过期时间 (SaaS 场景使用)", type = BizField.FieldType.DATETIME)
    private LocalDateTime expireAt;

    @TableField("created_at")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdAt;

    @TableField("created_by")
    @BizField(name = "创建人 userId", type = BizField.FieldType.STRING)
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



