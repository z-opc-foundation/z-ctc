package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.AccountDO;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 账号 VO — 不暴露 passwordHash/passwordSalt/credential 等敏感字段.
 */
public class AccountVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String accountNo;
    private String username;
    private String nickname;
    private Integer accountType;
    private String tenantCode;
    private Integer status;
    private String email;
    private String phone;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AccountVO from(AccountDO d) {
        if (d == null) { return null; }

        AccountVO v = new AccountVO();
        v.id = d.getId();
        v.accountNo = d.getAccountNo();
        v.username = d.getUsername();
        v.nickname = d.getNickname();
        v.accountType = d.getAccountType();
        v.tenantCode = d.getTenantCode();
        v.status = d.getStatus();
        v.email = d.getEmail();
        v.phone = d.getPhone();
        v.expireAt = d.getExpireAt();
        v.createdAt = d.getCreatedAt();
        v.updatedAt = d.getUpdatedAt();
        return v;
    }

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
