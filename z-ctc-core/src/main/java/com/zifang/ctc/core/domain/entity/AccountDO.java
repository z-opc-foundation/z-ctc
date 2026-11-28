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
 * 账号主表实体.
 */
@BizModel(name = "账号主表实体", module = "z-ctc", description = "账号主表实体.")
@TableName("z_ctc_ac_account")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class AccountDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("account_no")
    @BizField(name = "账号编号 (业务可读唯一标识)", type = BizField.FieldType.STRING)
    private String accountNo;

    @TableField("username")
    @BizField(name = "用户名 (登录名)", type = BizField.FieldType.STRING)
    private String username;

    @TableField("nickname")
    @BizField(name = "昵称", type = BizField.FieldType.STRING)
    private String nickname;

    @TableField("password_hash")
    @BizField(name = "BCrypt 密码哈希 (账号类型=密码登录时存)", type = BizField.FieldType.STRING)
    private String passwordHash;

    @TableField("password_salt")
    @BizField(name = "密码盐值 (历史兼容, BCrypt 自带盐, 一般不用)", type = BizField.FieldType.STRING)
    private String passwordSalt;

    @TableField("account_type")
    @BizField(name = "账号类型: 1=超级管理员 2=租户管理员 3=普通成员 4=临时账号", type = BizField.FieldType.INTEGER)
    private Integer accountType;

    @TableField("tenant_code")
    @BizField(name = "租户 (超级管理员/公共账号传 NULL)", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("status")
    @BizField(name = "状态: 0=禁用 1=正常 2=锁定", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("email")
    @BizField(name = "邮箱", type = BizField.FieldType.STRING)
    private String email;

    @TableField("phone")
    @BizField(name = "手机号", type = BizField.FieldType.STRING)
    private String phone;

    @TableField("expire_at")
    @BizField(name = "账号过期时间 (临时账号/试用期账号使用)", type = BizField.FieldType.DATETIME)
    private LocalDateTime expireAt;

    @TableField("last_login_at")
    @BizField(name = "最近登录时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime lastLoginAt;

    @TableField("last_login_ip")
    @BizField(name = "最近登录 IP", type = BizField.FieldType.STRING)
    private String lastLoginIp;

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

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
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



