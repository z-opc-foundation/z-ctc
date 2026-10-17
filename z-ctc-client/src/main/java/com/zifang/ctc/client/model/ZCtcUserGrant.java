package com.zifang.ctc.client.model;

import java.io.Serializable;

/**
 * 用户对权限点的直挂授权（白名单）。字段与 4A 中心的 {@code UserGrantDO} 对齐。
 *
 * <p>时间字段刻意用 {@code String}：消费方只做展示与「是否已授权」判断，
 * 不做时间算术，反序列化成本比引 JSR-310 低，也不必关心 4A 中心写入时的格式。
 */
public class ZCtcUserGrant implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long resourceId;
    private String tenantCode;
    private String expireAt;
    private String grantedAt;
    private String grantedBy;
    private String description;

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

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(String expireAt) {
        this.expireAt = expireAt;
    }

    public String getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(String grantedAt) {
        this.grantedAt = grantedAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
