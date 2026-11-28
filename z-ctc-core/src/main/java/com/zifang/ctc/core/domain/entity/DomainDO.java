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
 * 域实体.
 * <p>
 * 表: z_ctc_ac_domain — 每个租户下有多个域 (如 prod/staging/dev 环境隔离).
 * domainCode 在 Org/Dept/Group 表中作为引用字段，用于组织架构的租户内分组.
 */
@BizModel(name = "域实体", module = "z-ctc", description = "域实体. 表: z_ctc_ac_domain — 每个租户下有多个域 (如 prod/staging/dev 环境隔离). domainCode 在 Org/Dept/Group 表中作为引用字段，用于组织架构的租户内分组.")
@TableName("z_ctc_ac_domain")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class DomainDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("domain_code")
    @BizField(name = "域编码 (租户内唯一, 如 prod/staging)", type = BizField.FieldType.STRING)
    private String domainCode;

    @TableField("domain_name")
    @BizField(name = "域名称", type = BizField.FieldType.STRING)
    private String domainName;

    @TableField("tenant_code")
    @BizField(name = "所属租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("status")
    @BizField(name = "状态 1-启用 0-禁用", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("description")
    @BizField(name = "描述", type = BizField.FieldType.STRING)
    private String description;

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

