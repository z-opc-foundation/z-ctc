package com.zifang.ctc.core.vo;

import java.io.Serializable;

/**
 * 登录响应 VO — token + 账号信息.
 */
public class LoginResponseVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String token;
    private AccountVO account;

    public LoginResponseVO() {
    }

    public LoginResponseVO(String token, AccountVO account) {
        this.token = token;
        this.account = account;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AccountVO getAccount() {
        return account;
    }

    public void setAccount(AccountVO account) {
        this.account = account;
    }
}
