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
 * 用户直接授权 (白名单, 加集).
 * <p>
 * RBAC+Override 模型: 最终权限 = 角色权限(含继承) ∪ user_grant - user_deny.
 * <p>
 * 对应表 z_ctc_spc_user_grant.
 */
@BizModel(name = "用户直接授权 (白名单, 加集)", module = "z-ctc", description = "用户直接授权 (白名单, 加集). RBAC+Override 模型: 最终权限 = 角色权限(含继承) ∪ user_grant - user_deny. 对应表 z_ctc_spc_user_grant.")
@TableName("z_ctc_spc_user_grant")
public class UserGrantDO implements Serializable {

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

    @TableField("granted_at")
    @BizField(name = "授权时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime grantedAt;

    @TableField("granted_by")
    @BizField(name = "授权人", type = BizField.FieldType.STRING)
    private String grantedBy;

    @TableField("description")
    @BizField(name = "授权理由", type = BizField.FieldType.STRING)
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

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}



