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
 * 凭证实体. 一个账号可绑定多种凭证 (用户名+密码 / 手机+验证码 / 邮箱 / 微信 unionid 等).
 */
@BizModel(name = "凭证实体. 一个账号可绑定多种凭证 (用户名+密码 / 手机+验证码 / 邮箱 / 微信 unionid 等)", module = "z-ctc", description = "凭证实体. 一个账号可绑定多种凭证 (用户名+密码 / 手机+验证码 / 邮箱 / 微信 unionid 等).")
@TableName("z_ctc_ac_auth")
public class AuthDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("user_id")
    @BizField(name = "用户 ID (关联 z_ctc_ac_user.id)", type = BizField.FieldType.INTEGER)
    private Long userId;

    @TableField("identity_type")
    @BizField(name = "凭证类型: 1=密码 2=手机 3=邮箱 4=微信 unionid 5=钉钉 6=企业微信", type = BizField.FieldType.INTEGER)
    private Integer identityType;

    @TableField("identifier")
    @BizField(name = "凭证标识 (用户名/手机号/邮箱/unionid)", type = BizField.FieldType.STRING)
    private String identifier;

    @TableField("credential")
    @BizField(name = "凭证值 (密码哈希/手机验证码/unionid 完整值)", type = BizField.FieldType.STRING)
    private String credential;

    @TableField("salt")
    @BizField(name = "盐 (历史兼容)", type = BizField.FieldType.STRING)
    private String salt;

    @TableField("credential_strength")
    @BizField(name = "密码强度: 0=弱 1=中 2=强 (仅密码凭证使用)", type = BizField.FieldType.INTEGER)
    private Integer credentialStrength;

    @TableField("status")
    @BizField(name = "状态: 0=禁用 1=正常", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("created_at")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedAt;

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

    public Integer getIdentityType() {
        return identityType;
    }

    public void setIdentityType(Integer identityType) {
        this.identityType = identityType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Integer getCredentialStrength() {
        return credentialStrength;
    }

    public void setCredentialStrength(Integer credentialStrength) {
        this.credentialStrength = credentialStrength;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}



