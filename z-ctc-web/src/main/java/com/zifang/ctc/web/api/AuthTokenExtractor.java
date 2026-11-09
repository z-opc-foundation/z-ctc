package com.zifang.ctc.web.api;

import com.zifang.ctc.sso.config.SsoProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 免登录自解析端点共用的凭证提取: Authorization 头 > Cookie（按 {@code sso.token-cookie-names} 顺序）.
 *
 * <p>背景（2026-11-09, 「未登录或 token 缺少 userId」）: {@code /api/ctc/authn/**} 与
 * {@code /api/ctc/ac/invitations/**} 在 {@code sso.exclude-paths} 里收不到 SsoInterceptor 的
 * Cookie 鉴权, 而 z-team 顶栏切租户 / onboarding 走原生 fetch {@code credentials:include} ——
 * 只带 Cookie 不带 Authorization 头。此前 my-tenants / select-tenant / invitations 的
 * {@code extractUserId} 只认 Authorization 头 ⇒ Cookie 会话被误判成「未登录」。
 * whoami 的同款问题已在 FEATURE-ZTEAM-AUTH-UNIFY 修过, 这里把同款修法抽成共用件。</p>
 *
 * <p>刻意<b>不收</b> {@code ?token=} query 通道: 这些路径不在 SsoInterceptor 的
 * query-token 白名单里, token 进 URL 会落浏览器历史与网关 access log。</p>
 */
final class AuthTokenExtractor {

    private AuthTokenExtractor() {
    }

    /**
     * 从 Authorization 头或 SSO Cookie 中提取原始 token 文本; 两处都没有时返回 null.
     * <p>头优先: 带头的调用方（axios authRequest）语义不变; 只带 Cookie 的调用方
     * （fetch credentials:include）由此获得登录态。</p>
     */
    static String extract(HttpServletRequest request, SsoProperties ssoProperties) {
        if (request == null) {
            return null;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return authHeader;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null && ssoProperties != null) {
            List<String> candidateNames = ssoProperties.getEffectiveTokenCookieNames();
            for (String name : candidateNames) {
                for (Cookie c : cookies) {
                    if (name.equals(c.getName())) {
                        String v = c.getValue();
                        if (v != null && !v.isEmpty()) {
                            return v;
                        }
                    }
                }
            }
        }
        return null;
    }
}
