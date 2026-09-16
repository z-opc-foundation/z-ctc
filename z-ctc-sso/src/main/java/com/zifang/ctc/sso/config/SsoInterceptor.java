package com.zifang.ctc.sso.config;


import com.zifang.ctc.sso.model.UserInfo;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class SsoInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final SsoProperties ssoProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private List<String> excludedPaths;
    private List<String> interceptPaths;
    /**
     * 域名 → 租户/域 覆盖规则.
     * 优先于 token claim. 用途: 不同子域名强制不同上下文 (PPT → default + tool_domain).
     */
    private java.util.List<DomainOverride> domainOverrides;

    public SsoInterceptor(TokenService tokenService, SsoProperties ssoProperties) {
        this.tokenService = tokenService;
        this.ssoProperties = ssoProperties;
    }

    public void setExcludedPaths(List<String> excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    public void setInterceptPaths(List<String> interceptPaths) {
        this.interceptPaths = interceptPaths;
    }

    public void setDomainOverrides(java.util.List<DomainOverride> domainOverrides) {
        this.domainOverrides = domainOverrides;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String requestUri = request.getRequestURI();

        // 检查是否为忽略的路径
        if (isExcludePath(requestUri)) {
            return true;
        }

        // 检查是否需要拦截的路径
        if (!isInterceptPath(requestUri)) {
            return true;
        }

        // FEATURE057 改进: 尝试所有候选 cookie (按 sso.token-cookie-names 顺序),
        // 哪一个能验证成功就用哪一个. 解决浏览器带 idea-bfcfe0a7 等旧 SSO UUID cookie 时
        // 无法拦截的问题, 同时优先识别 sso_token / zctc_token 等新 JWT cookie.
        UserInfo userInfo = tryVerifyAllCookieTokens(request);
        if (userInfo == null) {
            // query param / Authorization header 也尝试一遍
            String headerToken = request.getParameter("token");
            if (headerToken == null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    headerToken = authHeader.substring(7);
                } else if (authHeader != null) {
                    headerToken = authHeader;
                }
            }
            if (headerToken != null && !headerToken.trim().isEmpty()) {
                userInfo = tokenService.verifyToken(headerToken);
            }
        }

        if (userInfo != null) {
            // token有效, 应用域名覆盖 (域名固定 → 强制租户/域)
            applyDomainOverride(request, userInfo);
            // 将用户信息存入请求属性
            request.setAttribute("ssoUser", userInfo);
            return true;
        }

        // token无效或不存在
        // API 请求（Accept: application/json）返回 401 JSON，浏览器请求重定向到登录页
        String accept = request.getHeader("Accept");
        boolean isApiRequest = (accept != null && accept.contains("application/json"))
                || (request.getHeader("Content-Type") != null && request.getHeader("Content-Type").contains("application/json"));
        if (isApiRequest) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":401,\"message\":\"未登录或Token无效\",\"data\":null}");
            return false;
        }
        String redirectUrl = ssoProperties.getLoginUrl() + "?redirect=" +
                request.getRequestURL().toString();
        response.sendRedirect(redirectUrl);
        return false;
    }

    /**
     * FEATURE057 改进: 按 sso.token-cookie-names 顺序遍历所有候选 cookie, 验证其 token.
     * 即使浏览器只带 idea-bfcfe0a7 这种旧 SSO UUID cookie, 也能正确处理.
     */
    private UserInfo tryVerifyAllCookieTokens(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        List<String> candidateNames = ssoProperties.getEffectiveTokenCookieNames();
        for (String target : candidateNames) {
            for (Cookie cookie : cookies) {
                if (!target.equals(cookie.getName())) {
                    continue;
                }
                String value = cookie.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                try {
                    UserInfo info = tokenService.verifyToken(value);
                    if (info != null) {
                        return info;
                    }
                } catch (Throwable ignore) {
                    // 单个 cookie 验证失败不影响其他
                }
            }
        }
        return null;
    }

    /**
     * 应用域名 → 租户/域 覆盖.
     * 优先级: Host header > X-Sso-Tenant/Domain header > token claim > 配置的 domainOverrides.
     */
    private void applyDomainOverride(HttpServletRequest request, UserInfo userInfo) {
        if (domainOverrides == null || domainOverrides.isEmpty()) {
            // 没有配置覆盖规则, 仍可从 header 强制覆盖 (nginx 层注入)
            overrideFromHeaders(request, userInfo);
            return;
        }
        String host = request.getHeader("Host");
        if (host == null) { return; }
        // 去掉端口
        int colonIdx = host.indexOf(':');
        String hostname = colonIdx > 0 ? host.substring(0, colonIdx) : host;
        // 优先从 header 强制 (overrides header)
        String hdrTenant = request.getHeader("X-Sso-Tenant");
        String hdrDomain = request.getHeader("X-Sso-Domain");
        if (hdrTenant != null && !hdrTenant.isEmpty()) {
            userInfo.setTenantCode(hdrTenant);
        }
        if (hdrDomain != null && !hdrDomain.isEmpty()) {
            userInfo.setDomainCode(hdrDomain);
        }
        // 然后按域名规则覆盖
        for (DomainOverride o : domainOverrides) {
            if (o.match(hostname)) {
                if (o.getTenantCode() != null && !o.getTenantCode().isEmpty()) {
                    userInfo.setTenantCode(o.getTenantCode());
                }
                if (o.getDomainCode() != null && !o.getDomainCode().isEmpty()) {
                    userInfo.setDomainCode(o.getDomainCode());
                }
                return;
            }
        }
    }

    private void overrideFromHeaders(HttpServletRequest request, UserInfo userInfo) {
        String hdrTenant = request.getHeader("X-Sso-Tenant");
        String hdrDomain = request.getHeader("X-Sso-Domain");
        if (hdrTenant != null && !hdrTenant.isEmpty()) {
            userInfo.setTenantCode(hdrTenant);
        }
        if (hdrDomain != null && !hdrDomain.isEmpty()) {
            userInfo.setDomainCode(hdrDomain);
        }
    }

    // 从Cookie中获取token
    // 按 SsoProperties.getEffectiveTokenCookieNames() 返回的候选名依次匹配,
    // 第一个命中的 cookie 值即为 token。兼容旧配置 (仅 tokenCookieName) 与新配置 (tokenCookieNames 多值).
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        List<String> candidateNames = ssoProperties.getEffectiveTokenCookieNames();
        // 先按候选名顺序匹配, 保证优先级 (tokenCookieName 在前, 多值列表在后)
        for (String target : candidateNames) {
            for (Cookie cookie : cookies) {
                if (target.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    // 判断是否为忽略的路径
    private boolean isExcludePath(String requestUri) {
        List<String> paths = (excludedPaths != null) ? excludedPaths : ssoProperties.getExcludePaths();
        for (String pattern : paths) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }

    // 判断是否为需要拦截的路径
    private boolean isInterceptPath(String requestUri) {
        List<String> paths = (interceptPaths != null) ? interceptPaths : ssoProperties.getInterceptPaths();
        for (String pattern : paths) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }
}