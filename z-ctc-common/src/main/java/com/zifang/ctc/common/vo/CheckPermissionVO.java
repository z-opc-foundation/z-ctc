package com.zifang.ctc.common.vo;

import java.io.Serializable;

/**
 * 权限检查 VO.
 */
public class CheckPermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String resourceCode;
    private Boolean allowed;

    public CheckPermissionVO() {
    }

    public CheckPermissionVO(Long userId, String resourceCode, Boolean allowed) {
        this.userId = userId;
        this.resourceCode = resourceCode;
        this.allowed = allowed;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public Boolean getAllowed() {
        return allowed;
    }

    public void setAllowed(Boolean allowed) {
        this.allowed = allowed;
    }
}