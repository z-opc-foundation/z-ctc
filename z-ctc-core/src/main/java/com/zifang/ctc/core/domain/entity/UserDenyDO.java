package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户直接拒绝 (黑名单, 减集).
 * <p>
 * RBAC+Override 模型: 最终权限 = 角色权限(含继承) ∪ user_grant - user_deny.
 * <p>
 * 对应表 z_ctc_spc_user_deny.
 */
@BizModel(name = "用户直接拒绝 (黑名单, 减集)", module = "z-ctc", description = "用户直接拒绝 (黑名单, 减集). RBAC+Override 模型: 最终权限 = 角色权限(含继承) ∪ user_grant - user_deny. 对应表 z_ctc_spc_user_deny.")
@TableName("z_ctc_spc_user_deny")
public class UserDenyDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("user_id")
    @BizField(name = "用户 ID (关联 z_ctc_ac_user.id)", type = BizField.FieldType.INTEGER)
    private Long userId;

    @TableField("resource_id")
    @BizField(name = "资源 ID (关联 z_ctc_spc_resource.id)", type = BizField.FieldType.INTEGER)
    private Long resourceId;

    @TableField("tenant_code")
    @BizField(name = "租户编码 (隔离)", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("expire_at")
    @BizField(name = "过期时间 (NULL=永久)", type = BizField.FieldType.DATETIME)
    private LocalDateTime expireAt;

    @TableField("denied_at")
    @BizField(name = "拒绝时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime deniedAt;

    @TableField("denied_by")
    @BizField(name = "操作人", type = BizField.FieldType.STRING)
    private String deniedBy;

    @TableField("description")
    @BizField(name = "拒绝理由", type = BizField.FieldType.STRING)
    private String description;

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }

    public LocalDateTime getDeniedAt() { return deniedAt; }
    public void setDeniedAt(LocalDateTime deniedAt) { this.deniedAt = deniedAt; }

    public String getDeniedBy() { return deniedBy; }
    public void setDeniedBy(String deniedBy) { this.deniedBy = deniedBy; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}



