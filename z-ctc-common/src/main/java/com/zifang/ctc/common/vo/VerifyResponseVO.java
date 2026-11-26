package com.zifang.ctc.common.vo;

import java.io.Serializable;
import java.util.Map;

/**
 * verify token 响应 VO.
 */
public class VerifyResponseVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Object> claims;
    private boolean valid;

    public VerifyResponseVO() {
    }

    public VerifyResponseVO(Map<String, Object> claims, boolean valid) {
        this.claims = claims;
        this.valid = valid;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
