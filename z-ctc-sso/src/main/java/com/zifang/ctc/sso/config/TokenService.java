package com.zifang.ctc.sso.config;

import com.zifang.ctc.sso.model.UserInfo;

public interface TokenService {
    /**
     * 验证token有效性
     *
     * @param token 令牌
     * @return 有效的用户信息，无效则返回null
     */
    UserInfo verifyToken(String token);
}