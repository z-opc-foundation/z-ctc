package com.zifang.ctc.sso.config;


import com.zifang.ctc.sso.model.UserInfo;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class SsoContext {

    private SsoContext() {
        // 私有构造，防止实例化
    }

    private static volatile TokenService tokenService;

    /**
     * FEATURE057 增强: 注入 TokenService bean, 让 controller 端可独立调用 verifyToken.
     * 由 SsoAutoConfiguration 在 @Configuration 阶段调用一次.
     */
    public static void init(TokenService tokenService) {
        SsoContext.tokenService = tokenService;
    }

    /**
     * FEATURE057 增强: 提供静态入口, 供 TeamCurrentUserResolver.resolve 等场景使用
     * 当 cookie 中携带 sso_token / zctc_token JWT 时, 直接解析并返回 UserInfo.
     */
    public static UserInfo verifyTokenFromCookie(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        TokenService svc = tokenService;
        if (svc == null) {
            return null;
        }
        try {
            return svc.verifyToken(token);
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息，未登录则返回null
     */
    public static UserInfo getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return (UserInfo) request.getAttribute("ssoUser");
    }

    /**
     * 判断当前用户是否已登录
     *
     * @return 是否登录
     */
    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }
}