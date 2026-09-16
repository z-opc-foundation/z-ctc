package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.RoleDO;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色 VO.
 */
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String roleCode;
    private String roleName;
    private Long parentRoleId;
    private String tenantCode;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;

    public RoleVO() {
    }

    public static RoleVO from(RoleDO roleDO) {
        if (roleDO == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        vo.setId(roleDO.getId());
        vo.setRoleCode(roleDO.getRoleCode());
        vo.setRoleName(roleDO.getRoleName());
        vo.setParentRoleId(roleDO.getParentRoleId());
        vo.setTenantCode(roleDO.getTenantCode());
        vo.setDescription(roleDO.getDescription());
        vo.setStatus(roleDO.getStatus());
        vo.setCreatedAt(roleDO.getCreatedAt());
        vo.setCreatedBy(roleDO.getCreatedBy());
        vo.setUpdatedAt(roleDO.getUpdatedAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Long getParentRoleId() {
        return parentRoleId;
    }

    public void setParentRoleId(Long parentRoleId) {
        this.parentRoleId = parentRoleId;
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