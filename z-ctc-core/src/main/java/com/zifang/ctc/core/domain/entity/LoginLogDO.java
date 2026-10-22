package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体.
 */
@TableName("z_ctc_ac_login_log")
public class LoginLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("account_id")
    private Long accountId;

    @TableField("identifier")
    private String identifier;

    @TableField("identity_type")
    private Integer identityType;

    @TableField("login_status")
    private Integer loginStatus;

    @TableField("failure_reason")
    private String failureReason;

    /**
     * 客户端真实来源 IP（卡 TASK-20261022-001 第 2 项）。
     * <p>
     * 语义：**服务端看到的真实来源地址** —— 对端地址（{@code getRemoteAddr()}）+
     * {@code X-Forwarded-For} 链，**以最后一跳可信地址为准**。
     * <p>
     * ⚠ 历史上本字段只从 {@code X-Client-Ip} **请求头**取值 ⇒ 没有组件注入该头时
     * **恒为 NULL**（实测 1863 行 100% 为 NULL）⇒ 字段存在 ≠ 能按 IP 归因。
     * 现在写入路径已改为取真实来源（见 {@code SsoInterceptor.resolveClientIp}）。
     */
    @TableField("client_ip")
    private String clientIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("login_time")
    private LocalDateTime loginTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Integer getIdentityType() {
        return identityType;
    }

    public void setIdentityType(Integer identityType) {
        this.identityType = identityType;
    }

    public Integer getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(Integer loginStatus) {
        this.loginStatus = loginStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
}
