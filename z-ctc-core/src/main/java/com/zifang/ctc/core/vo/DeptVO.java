package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.DeptDO;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DeptVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantCode;
    private String domainCode;
    private String orgCode;
    private String deptCode;
    private String deptName;
    /**
     * 父部门编码；根部门为 null。
     *
     * <p>2026-10-04 补齐。此前 {@link DeptDO} 有这个字段而本 VO 没有，
     * {@link #from(DeptDO)} 也没映射 ⇒ {@code GET /api/ctc/ac/depts/list}
     * 返回的每条部门记录都丢掉父级，调用方拿不到层级、只能平铺渲染，
     * 部门树在 API 出口这一环断掉。
     */
    private String parentCode;
    private Integer status;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;

    public static DeptVO from(DeptDO d) {
        if (d == null) { return null; }

        DeptVO v = new DeptVO();
        v.id = d.getId();
        v.tenantCode = d.getTenantCode();
        v.domainCode = d.getDomainCode();
        v.orgCode = d.getOrgCode();
        v.deptCode = d.getDeptCode();
        v.deptName = d.getDeptName();
        v.parentCode = d.getParentCode();
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
