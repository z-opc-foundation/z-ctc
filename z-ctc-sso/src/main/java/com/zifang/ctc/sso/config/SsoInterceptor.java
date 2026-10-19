package com.zifang.ctc.sso.config;


import com.zifang.ctc.sso.model.UserInfo;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SsoInterceptor implements HandlerInterceptor {

    /**
     * 免登录标记的简单名。本仓不依赖各业务仓（z-team-common 的 {@code @NoLogin} 不能被这里编译期引用），
     * 故按简单名识别；未标注该注解的端点行为与从前完全一致。
     */
    private static final String NO_LOGIN_ANNOTATION_SIMPLE_NAME = "NoLogin";

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

        // 端点自身标注免登录标记（业务仓自持，注解是唯一真源，避免与 exclude-paths 配置两处口径漂移）
        if (isNoLoginAnnotated(handler)) {
            return true;
        }

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
        String redirectUrl = ssoProperties.getLoginUrl() + "?redirect=" + encode(originalUrl(request));
        response.sendRedirect(redirectUrl);
        return false;
    }

    /**
     * 还原用户当初请求的完整 URL，作为登录后的回跳目标。
     * <p>
     * {@code request.getRequestURL()} 拿到的是**容器看到的**路径，也就是网关剥完前缀之后
     * 的样子。经 Traefik 的 kapi-strip-ctc 中间件进来时，前缀 {@code /kapi/z-ctc} 已经被
     * 去掉，于是拼出来的是 {@code https://opc.zopc.top/api/ctc/authn/whoami} —— 这个路径在
     * opc 上根本不存在，登录成功后再跳过去只会 404。StripPrefix 中间件据文档会把被剥掉的
     * 那段放进 {@code X-Forwarded-Prefix}，这里据此补回去；该头不存在时退化成原行为。
     * <p>
     * 直连容器（无网关）时该头不存在，补前缀逻辑自然跳过，行为与从前一致。
     */
    private String originalUrl(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String stripped = request.getHeader("X-Forwarded-Prefix");
        String query = request.getQueryString();
        String suffix = (query == null || query.isEmpty()) ? "" : "?" + query;
        if (stripped == null || stripped.isEmpty() || "/".equals(stripped)) {
            return url + suffix;
        }
        String prefix = stripped.endsWith("/")
                ? stripped.substring(0, stripped.length() - 1)
                : stripped;
        return url.substring(0, url.length() - request.getRequestURI().length()) + prefix + suffix;
    }

    /**
     * URLEncoder 把空格编成 {@code +}，而 query 值里的 {@code +} 会被接收端解回空格，
     * 于是带空格的回跳地址到对面就散了。编完把 {@code +} 换回 {@code %20}。
     */
    private static String encode(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 不可用", e);
        }
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

    /** 处理器方法或其所在类上是否带免登录标记 */
    private boolean isNoLoginAnnotated(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }
        HandlerMethod hm = (HandlerMethod) handler;
        return hasNoLogin(hm.getMethod()) || hasNoLogin(hm.getBeanType());
    }

    private boolean hasNoLogin(java.lang.reflect.AnnotatedElement element) {
        for (Annotation ann : element.getAnnotations()) {
            if (NO_LOGIN_ANNOTATION_SIMPLE_NAME.equals(ann.annotationType().getSimpleName())) {
                return true;
            }
        }
        return false;
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