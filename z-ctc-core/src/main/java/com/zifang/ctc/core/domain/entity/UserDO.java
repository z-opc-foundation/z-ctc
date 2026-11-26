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
 * z-ctc 全局用户 (FEATURE049).
 * <p>
 * 身份层实体, 不绑租户. 一个真实的人 = 一个全局 User.
 * User 与 Tenant 的关系通过 z_ctc_ac_membership 多对多表达.
 * <p>
 * 凭证 (用户名+密码 / 手机+验证码 / 邮箱 / 微信) 挂在 z_ctc_ac_auth.user_id 上.
 */
@BizModel(name = "z-ctc 全局用户", module = "z-ctc", description = "z-ctc 全局用户 (FEATURE049). 身份层实体, 不绑租户. 一个真实的人 = 一个全局 User. User 与 Tenant 的关系通过 z_ctc_ac_membership 多对多表达. 凭证 (用户名+密码 / 手机+验证码 / 邮箱 / 微信) 挂在 z_ctc_ac_auth.user_id 上.")
@TableName("z_ctc_ac_user")
public class UserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("user_no")
    @BizField(name = "用户编号 (业务可读唯一标识)", type = BizField.FieldType.STRING)
    private String userNo;

    @TableField("email")
    @BizField(name = "邮箱 (全局唯一, 登录用)", type = BizField.FieldType.STRING)
    private String email;

    @TableField("phone")
    @BizField(name = "手机号 (全局唯一, 登录用)", type = BizField.FieldType.STRING)
    private String phone;

    @TableField("username")
    @BizField(name = "用户名 (可选, 非唯一, 仅显示用)", type = BizField.FieldType.STRING)
    private String username;

    @TableField("nickname")
    @BizField(name = "昵称", type = BizField.FieldType.STRING)
    private String nickname;

    @TableField("avatar")
    @BizField(name = "头像 URL", type = BizField.FieldType.STRING)
    private String avatar;

    @TableField("password_hash")
    @BizField(name = "BCrypt 密码哈希", type = BizField.FieldType.STRING)
    private String passwordHash;

    /**
     * 全局状态: 0=禁用 1=正常 2=锁定
     */
    @TableField("status")
    @BizField(name = "全局状态: 0=禁用 1=正常 2=锁定", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("last_login_at")
    @BizField(name = "最近登录时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime lastLoginAt;

    @TableField("last_login_ip")
    @BizField(name = "最近登录 IP", type = BizField.FieldType.STRING)
    private String lastLoginIp;

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

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
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



