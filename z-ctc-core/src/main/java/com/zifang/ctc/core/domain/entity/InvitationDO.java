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
 * z-ctc 租户邀请记录 (FEATURE049).
 * <p>
 * 邀请方式:
 * - 邀请链接: 带 token (UUID), 精准定向
 * - 邀请码: 短码 (6-8位), 手动输入, 可设 max_uses > 1 批量邀请
 * <p>
 * status:
 * 0=待接受 1=已接受 2=已拒绝 3=已过期
 */
@BizModel(name = "z-ctc 租户邀请记录", module = "z-ctc", description = "z-ctc 租户邀请记录 (FEATURE049). 邀请方式: - 邀请链接: 带 token (UUID), 精准定向 - 邀请码: 短码 (6-8位), 手动输入, 可设 max_uses > 1 批量邀请 status: 0=待接受 1=已接受 2=已拒绝 3=已过期")
@TableName("z_ctc_ac_invitation")
public class InvitationDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("tenant_code")
    @BizField(name = "邀请来源租户", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("inviter_user_id")
    @BizField(name = "邀请人 user_id", type = BizField.FieldType.INTEGER)
    private Long inviterUserId;

    @TableField("invitee_email")
    @BizField(name = "被邀请人邮箱 (邀请链接用)", type = BizField.FieldType.STRING)
    private String inviteeEmail;

    @TableField("invitee_phone")
    @BizField(name = "被邀请人手机号", type = BizField.FieldType.STRING)
    private String inviteePhone;

    @TableField("invite_code")
    @BizField(name = "邀请码 (手动输入用, 6-8位)", type = BizField.FieldType.STRING)
    private String inviteCode;

    @TableField("token")
    @BizField(name = "邀请链接 token (UUID)", type = BizField.FieldType.STRING)
    private String token;

    @TableField("role_code")
    @BizField(name = "指定角色", type = BizField.FieldType.STRING)
    private String roleCode;

    /**
     * 0=待接受 1=已接受 2=已拒绝 3=已过期
     */
    @TableField("status")
    @BizField(name = "状态: 0=待接受 1=已接受 2=已拒绝 3=已过期", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("max_uses")
    @BizField(name = "最大使用次数 (邀请码可复用时>1)", type = BizField.FieldType.INTEGER)
    private Integer maxUses;

    @TableField("used_count")
    @BizField(name = "已使用次数", type = BizField.FieldType.INTEGER)
    private Integer usedCount;

    @TableField("expire_at")
    @BizField(name = "过期时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime expireAt;

    @TableField("accepted_at")
    @BizField(name = "接受时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime acceptedAt;

    @TableField("created_at")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdAt;

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

    public Long getInviterUserId() {
        return inviterUserId;
    }

    public void setInviterUserId(Long inviterUserId) {
        this.inviterUserId = inviterUserId;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public String getInviteePhone() {
        return inviteePhone;
    }

    public void setInviteePhone(String inviteePhone) {
        this.inviteePhone = inviteePhone;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}



