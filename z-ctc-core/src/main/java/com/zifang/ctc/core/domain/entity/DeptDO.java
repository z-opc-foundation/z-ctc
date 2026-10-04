package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门实体 (挂在组织下).
 */
@TableName("z_ctc_ac_dept")
public class DeptDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("domain_code")
    private String domainCode;

    @TableField("org_code")
    private String orgCode;

    @TableField("dept_code")
    private String deptCode;

    @TableField("dept_name")
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
    private String parentCode;

    @TableField("status")
    private Integer status;

    @TableField("description")
    private String description;

    @TableField("ext_config")
    private String extConfig;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("updated_by")
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