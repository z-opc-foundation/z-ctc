package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

@BizModel(name = "z_ctc_spc_user_role", module = "z-ctc", description = "z_ctc_spc_user_role")
@TableName("z_ctc_spc_user_role")
public class UserRoleDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("user_id")
    @BizField(name = "用户 ID (关联 z_ctc_ac_account.id)", type = BizField.FieldType.INTEGER)
    private Long userId;

    @TableField("role_id")
    @BizField(name = "角色 ID", type = BizField.FieldType.INTEGER)
    private Long roleId;

    /**
     * FEATURE049: 按租户隔离, 三元组唯一键 (user_id, role_id, tenant_code)
     */
    @TableField("tenant_code")
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("granted_at")
    @BizField(name = "授权时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime grantedAt;

    @TableField("granted_by")
    @BizField(name = "授权人", type = BizField.FieldType.STRING)
    private String grantedBy;

    @TableField("expire_at")
    @BizField(name = "过期时间 (临时授权场景)", type = BizField.FieldType.DATETIME)
    private LocalDateTime expireAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }
}



