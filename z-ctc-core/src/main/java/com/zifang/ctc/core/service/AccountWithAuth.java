package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.AuthDO;

/**
 * 账号 + 主凭证 (用户名+密码) 联合结果.
 */
public class AccountWithAuth {
    public final AccountDO account;
    public final AuthDO auth;

    public AccountWithAuth(AccountDO account, AuthDO auth) {
        this.account = account;
        this.auth = auth;
    }
}
