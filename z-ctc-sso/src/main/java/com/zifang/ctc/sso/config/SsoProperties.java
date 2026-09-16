package com.zifang.ctc.sso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "sso")
public class SsoProperties {

    // 登录页地址
    private String loginUrl = "http://localhost:8080/login";
    // token在Cookie中的名称（单值，兼容旧配置）
    private String tokenCookieName = "sso_token";
    // token在Cookie中的候选名称列表（多值，优先于 tokenCookieName；与 tokenCookieName 并存时取并集去重）
    private List<String> tokenCookieNames = new ArrayList<>();

    // 拦截的路径模式
    private List<String> interceptPaths = new ArrayList<String>() {{
        add("/**");
    }};

    // 忽略拦截的路径模式
    private List<String> excludePaths = new ArrayList<>();

    // token验证服务地址
    private String authServerUrl = "http://localhost:8080/auth/verify";

    // 域名 → 租户/域 覆盖规则 (YAML: sso.domain-overrides)
    private List<DomainOverride> domainOverrides = new ArrayList<>();

    // Cookie Domain (跨子域共享 SSO token, 例如 ".zopc.top" 让 opc/ppt 共享)
    // 留空则不设 Domain, 仅当前子域可用
    private String cookieDomain = "";
    // Cookie Secure (HTTPS 站点应设 true; HTTP 测试可设 false)
    private boolean cookieSecure = true;

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getTokenCookieName() {
        return tokenCookieName;
    }

    public void setTokenCookieName(String tokenCookieName) {
        this.tokenCookieName = tokenCookieName;
    }

    public List<String> getTokenCookieNames() {
        return tokenCookieNames;
    }

    public void setTokenCookieNames(List<String> tokenCookieNames) {
        this.tokenCookieNames = tokenCookieNames == null ? new ArrayList<>() : tokenCookieNames;
    }

    /**
     * 合并 tokenCookieName（单值）与 tokenCookieNames（多值），去重并保留顺序，
     * 返回最终生效的 token cookie 候选名列表。
     * 始终保证 tokenCookieName 出现在结果中（若 tokenCookieNames 包含则去重）。
     */
    public List<String> getEffectiveTokenCookieNames() {
        java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<>();
        if (tokenCookieName != null && !tokenCookieName.trim().isEmpty()) {
            ordered.add(tokenCookieName.trim());
        }
        if (tokenCookieNames != null) {
            for (String n : tokenCookieNames) {
                if (n != null && !n.trim().isEmpty()) {
                    ordered.add(n.trim());
                }
            }
        }
        if (ordered.isEmpty()) {
            ordered.add("sso_token");
        }
        return new ArrayList<>(ordered);
    }

    public List<String> getInterceptPaths() {
        return interceptPaths;
    }

    public void setInterceptPaths(List<String> interceptPaths) {
        this.interceptPaths = interceptPaths;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public List<DomainOverride> getDomainOverrides() {
        return domainOverrides;
    }

    public void setDomainOverrides(List<DomainOverride> domainOverrides) {
        this.domainOverrides = domainOverrides;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }
}
