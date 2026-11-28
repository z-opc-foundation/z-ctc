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
 * 部门实体 (挂在组织下).
 */
@BizModel(name = "部门实体 (挂在组织下)", module = "z-ctc", description = "部门实体 (挂在组织下).")
@TableName("z_ctc_ac_dept")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class DeptDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("tenant_code")
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("domain_code")
    @BizField(name = "域编码", type = BizField.FieldType.STRING)
    private String domainCode;

    @TableField("org_code")
    @BizField(name = "所属组织编码", type = BizField.FieldType.STRING)
    private String orgCode;

    @TableField("dept_code")
    @BizField(name = "部门编码 (租户域内唯一)", type = BizField.FieldType.STRING)
    private String deptCode;

    @TableField("dept_name")
    @BizField(name = "部门名称", type = BizField.FieldType.STRING)
    private String deptName;

    /**
     * 父部门编码；根部门为 null。
     *
     * <p><b>2026-10-04 新增</b>。此前 {@code z_ctc_ac_dept} 表<b>没有父级列</b>，
     * 而并列的旧表 {@code z_ctc_dept} 有 {@code parent_code} —— 于是本仓
     * {@code DeptDO} 无从表达层级，<b>部门树退化成平铺列表</b>。
     * 对照证据：{@code _doc/002_deploy/init/code-based-migration.sql} 曾把
     * {@code z_ctc_dept} 的层级迁成 {@code dept_code + parent_code} 三元组，
     * 但 ac 体系（新表）没跟上。
     *
     * <p>配套迁移：{@code z_ctc_ac_dept_add_parent_code.sql}（加列 + 回填自引用检查）。
     */
    @TableField("parent_code")
    @BizField(name = "父部门编码；根部门为 null", type = BizField.FieldType.STRING)
    private String parentCode;

    @TableField("status")
    @BizField(name = "状态: 0=停用 1=正常", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("description")
    @BizField(name = "描述", type = BizField.FieldType.STRING)
    private String description;

    @TableField("ext_config")
    @BizField(name = "扩展配置 (JSON 字符串)", type = BizField.FieldType.STRING)
    private String extConfig;

    @TableField("created_at")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdAt;

    @TableField("created_by")
    @BizField(name = "创建人 userId", type = BizField.FieldType.STRING)
    private String createdBy;

    @TableField("updated_at")
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedAt;

    @TableField("updated_by")
    @BizField(name = "更新人 userId", type = BizField.FieldType.STRING)
    private String updatedBy;

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

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
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

    public String getExtConfig() {
        return extConfig;
    }

    public void setExtConfig(String extConfig) {
        this.extConfig = extConfig;
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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

