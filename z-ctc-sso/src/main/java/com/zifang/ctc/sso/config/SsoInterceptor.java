package com.zifang.ctc.sso.config;


import com.zifang.ctc.sso.model.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SsoInterceptor implements HandlerInterceptor {

    /**
     * 闸门拒绝日志（卡 TASK-20261022-001 第 1 项 b 子项）。
     * <p>
     * <b>为什么必须有</b>：本类此前**一条日志语句都没有** —— 闸门拒绝时 401 直接返回，
     * 链路上**没有一行字** ⇒ 事后「查不到异常登录」根本不能当没被攻击过的免罪符。
     * <p>
     * <b>记什么</b>：URL + 客户端 IP + 拒绝原因（**不记 token 值**，只记「有没有带/哪种通道」）。
     */
    private static final Logger log = LogManager.getLogger(SsoInterceptor.class);

    /**
     * 免登录标记的简单名。本仓不依赖各业务仓（z-team-common 的 {@code @NoLogin} 不能被这里编译期引用），
     * 故按简单名识别；未标注该注解的端点行为与从前完全一致。
     */
    private static final String NO_LOGIN_ANNOTATION_SIMPLE_NAME = "NoLogin";

    /**
     * 允许「query 传 token」的路径白名单（2026-10-21，TASK-20260924-017）。
     *
     * <p><b>为什么要有白名单</b>：query 里的 token 会同时落进三处会长期留存的地方 ——
     * 浏览器历史、Nginx / 网关 access log、任何 {@code Referer} 出口。常规业务接口
     * （如 {@code /z-team/api/personal-agents/**}）完全没有理由走这条路：Cookie 与
     * {@code Authorization: Bearer} 都能带凭证，且后者根本不进 URL。
     *
     * <p><b>为什么不是零</b>：下面两条是「自己就会从 Authorization/Cookie/Query 三处
     * 解析 token」的端点（{@code AuthnController#extractUserFromRequest}），
     * 且 z-ctc-admin 侧不在 {@code sso.exclude-paths} 里、仍在本拦截器的拦截面内 ——
     * 把它们挡在闸门外会直接造成 401 回归。
     *
     * <p><b>为什么模式带「双星号通配前缀」</b>：同一个部署形态里 request URI 可能带
     * {@code /z-team}（context-path）或 {@code /kapi/z-ctc}（网关前缀未剥）前缀，
     * 用「斜杠 + 双星号 + 斜杠」前缀让同一组规则在三种前缀下都成立。
     * （写成字面量会在 javadoc 里提前闭合注释，故此处用文字描述。）
     *
     * <p><b>扩展点</b>：新增白名单条目请在本常量里加，并写清「为什么不能用请求头」。
     * {@code SsoAutoConfiguration} 的装配归 z-ctc 仓自己维护（该文件另有在办任务），
     * 故这里做成常量 + {@link #setQueryTokenAllowedPaths(List)}，不依赖配置项。
     */
    private static final List<String> QUERY_TOKEN_ALLOWED_PATHS = Collections.unmodifiableList(
            Arrays.asList(
                    // 当前用户查询：前端 z-team ctc-sso.ts / fetchMe 已改走 Cookie，
                    // 这里放行只为兼容「外部系统用 ?token= 查一次用户」的既有用法。
                    "/**/api/ctc/authn/whoami",
                    // 退出登录：清 Cookie 兜底端点，允许用 ?token= 定位要清的会话。
                    "/**/api/ctc/authn/logout"
            ));

    /**
     * query token 白名单的运行时覆盖（可选）。{@code null}/空 ⇒ 用上面的默认常量。
     * 供 z-ctc 仓后续把它接到配置上时使用。
     */
    private List<String> queryTokenAllowedPaths;

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

    /**
     * 覆盖「query 传 token」白名单（可选，默认见 {@link #QUERY_TOKEN_ALLOWED_PATHS}）。
     * 传 null / 空列表 ⇒ 回落到默认常量。
     */
    public void setQueryTokenAllowedPaths(List<String> paths) {
        this.queryTokenAllowedPaths = (paths == null || paths.isEmpty()) ? null : paths;
    }

    /**
     * 从 Authorization 头取 token：支持 {@code Bearer xxx} 与裸 token 两种历史写法。
     */
    private String bearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }

    /**
     * 该路径是否允许「query 传 token」。不在白名单 ⇒ query 里的 token 一律不认。
     */
    private boolean isQueryTokenAllowed(String requestUri) {
        List<String> patterns = (queryTokenAllowedPaths != null)
                ? queryTokenAllowedPaths
                : QUERY_TOKEN_ALLOWED_PATHS;
        for (String pattern : patterns) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
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
            // ① Authorization 头优先（推荐通道：Bearer 不进 URL、不进 access log、不进 Referer）
            String headerToken = bearerToken(request);
            // ② query param 只在显式白名单路径上才认（2026-10-21，TASK-20260924-017 收窄）
            if (headerToken == null && isQueryTokenAllowed(requestUri)) {
                headerToken = request.getParameter("token");
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
        // ⚠ 闸门拒绝**必须留痕**（卡 TASK-20261022-001 第 1 项 b 子项）：
        //   URL + 客户端 IP + 拒绝原因 —— 缺任何一样，事后都无法定位到「那次请求」。
        //   ⚠ **不记 token 值**（只记「带了没有 / 走的哪个通道」），避免把凭证写进日志。
        String rejectReason = describeRejectReason(request, requestUri);
        String clientIp = resolveClientIp(request);
        log.warn("[SSO-GATE] 拒绝: url={}, method={}, clientIp={}, reason={}, ua={}",
                requestUri, request.getMethod(), clientIp, rejectReason,
                request.getHeader("User-Agent"));

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
     * 拒绝原因（**不泄露凭证内容**，只说「带了没有、走哪条通道」）。
     * <p>
     * 分三档，便于事后区分「用户没登录」与「有人在拿假 token 撞闸门」：
     * <ul>
     *   <li>未携带任何凭证 —— Cookie / Authorization / query 三处都没有；</li>
     *   <li>携带了 Cookie 凭证但校验不过 —— 多半是过期，也可能是伪造；</li>
     *   <li>携带了 Authorization / query 凭证但校验不过 —— 多半是伪造 token。</li>
     * </ul>
     */
    private String describeRejectReason(HttpServletRequest request, String requestUri) {
        String headerToken = bearerToken(request);
        boolean headerPresent = headerToken != null && !headerToken.trim().isEmpty();
        boolean queryPresent = false;
        if (!headerPresent && isQueryTokenAllowed(requestUri)) {
            String q = request.getParameter("token");
            queryPresent = q != null && !q.trim().isEmpty();
        }
        if (headerPresent) {
            return "凭证无效或已过期（Authorization 头）";
        }
        if (queryPresent) {
            return "凭证无效或已过期（query token，白名单路径）";
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            List<String> candidates = ssoProperties.getEffectiveTokenCookieNames();
            for (Cookie c : cookies) {
                if (c != null && candidates.contains(c.getName())
                        && c.getValue() != null && !c.getValue().isEmpty()) {
                    return "凭证无效或已过期（Cookie " + c.getName() + "）";
                }
            }
        }
        return "未携带凭证（Cookie / Authorization / query 三处都没有）";
    }

    /**
     * 取客户端真实来源 IP（卡 TASK-20261022-001 第 2 项）。
     * <p>
     * <b>为什么不能只看请求头</b>：此前登录日志的 {@code client_ip} 只从
     * {@code X-Client-Ip} 请求头取值 ⇒ 没有任何组件注入这个头时**恒为 NULL**
     * （实测 1863 行 100% 为 NULL）⇒ 字段在、却**按 IP 归因不可用**。
     * <p>
     * <b>口径</b>（服务端真实对端地址 + XFF 链，**以最后一跳可信地址为准**）：
     * <ol>
     *   <li>对端地址 = {@code request.getRemoteAddr()}（容器看到的真实连接方，**不可伪造**）；</li>
     *   <li>XFF 链 = {@code X-Forwarded-For} 的逗号分隔列表（**左=原始客户端，右=离我们最近的一跳**）；
     *       边缘/网关会把它自己看到的地址**追加**到链尾 ⇒ 链尾那一项才是可信的。</li>
     *   <li>从**右往左**找第一个**非私网**地址 —— 我们自己的边缘/网关都在私网段，
     *       跳过它们后拿到的就是真实客户端；链上被伪造的前置项（更靠左）自然被忽略。</li>
     *   <li>整条链都是私网（客户端本身在内网）⇒ 退回 XFF **最左**那一项；
     *       连 XFF 都没有 ⇒ 只能用对端地址（此时只能定位到「最后一跳」，不是原始客户端）。</li>
     * </ol>
     * 返回 {@code null} 只可能发生在请求对象为 null 时。
     */
    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        List<String> chain = new ArrayList<>();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null) {
            for (String part : xff.split(",")) {
                String p = part.trim();
                if (!p.isEmpty()) {
                    chain.add(p);
                }
            }
        }
        String peer = request.getRemoteAddr();
        if (peer != null && !peer.trim().isEmpty()) {
            chain.add(peer.trim());
        }
        if (chain.isEmpty()) {
            return null;
        }
        // 从右往左：第一个非私网地址 = 最后一跳可信地址
        for (int i = chain.size() - 1; i >= 0; i--) {
            String ip = chain.get(i);
            if (looksLikeIp(ip) && !isPrivateAddress(ip)) {
                return ip;
            }
        }
        // 全是私网 ⇒ 退回 XFF 最左一项（客户端可能就在内网）
        return chain.get(0);
    }

    /** 粗判是否像 IP（v4 点分 / v6 十六进制）—— 只为剔除 XFF 里的垃圾值。 */
    private static boolean looksLikeIp(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                    || c == '.' || c == ':';
            if (!ok) {
                return false;
            }
        }
        return value.indexOf(':') > 0 || value.indexOf('.') > 0;
    }

    /** 是否私网/环回/链路本地地址（我们自己的边缘机、网关、Traefik 都在这些段里）。 */
    private static boolean isPrivateAddress(String value) {
        String v = value.toLowerCase();
        if (v.startsWith("127.") || v.equals("::1") || v.startsWith("fe80:")
                || v.startsWith("fc") || v.startsWith("fd")) {
            return true;
        }
        if (v.startsWith("10.") || v.startsWith("192.168.")) {
            return true;
        }
        if (v.startsWith("169.254.")) {
            return true;
        }
        if (v.startsWith("172.")) {
            String[] parts = v.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignore) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 还原用户当初请求的完整 URL，作为登录后的回跳目标。
     * <p>
     * {@code request.getRequestURL()} 拿到的是**容器看到的**样子，有两处与用户实际访问的不符：
     * <ul>
     *   <li>路径：经 Traefik 的 kapi-strip-ctc 中间件进来时前缀 {@code /kapi/z-ctc} 已被剥掉，
     *       拼出来的是 {@code https://opc.zopc.top/api/ctc/authn/whoami}，这路径在 opc 上不存在，
     *       登录成功后再跳过去只会 404。StripPrefix 会把剥掉的那段放进 {@code X-Forwarded-Prefix}，
     *       这里据此补回去。</li>
     *   <li>协议：容器只听 http，{@code getRequestURL()} 恒为 {@code http://}，哪怕公网入口是
     *       https。回跳地址写成 http 就得多走一跳 301 回 https，且明文暴露跳转目标。
     *       这里取 {@code sso.login-url} 的协议。</li>
     * </ul>
     * 路径那一项的头不存在时退化成原行为，直连容器的形态不受影响。
     * <p>
     * 协议这一项**刻意不读** {@code X-Forwarded-Proto}：公网入口是
     * 「边缘 nginx(TLS) → frps → frpc → Traefik 明文入口 → 容器」，而 frp 的 {@code type = "http"}
     * 代理**不转发 X-Forwarded-Proto**（实测直打 frps:8000，带与不带该头输出逐字节相同），
     * 头在隧道里就没了，Traefik 侧再配 forwardedHeaders 也无从生效。
     * {@code sso.login-url} 是运维对"公网协议"的显式声明，比从转发头反推更可靠，
     * 也不依赖任何网关行为。
     * <p>
     * 这里刻意**不开** {@code server.forward-headers-strategy}：那会让 ForwardedHeaderFilter
     * 接管 {@code X-Forwarded-Prefix} 并改写 {@code getRequestURI()}，本类按 {@code /api/ctc/**}
     * 口径匹配的 exclude/intercept 路径会全部失配，SSO 闸直接失效。
     */
    private String originalUrl(HttpServletRequest request) {
        String scheme = loginUrlScheme();
        String authority = authority(request, scheme);
        String uri = request.getRequestURI();
        String stripped = request.getHeader("X-Forwarded-Prefix");
        if (stripped != null && !stripped.isEmpty() && !"/".equals(stripped)) {
            uri = (stripped.endsWith("/") ? stripped.substring(0, stripped.length() - 1) : stripped) + uri;
        }
        String query = stripTokenParam(request.getQueryString());
        return scheme + "://" + authority + uri
                + ((query == null || query.isEmpty()) ? "" : "?" + query);
    }

    /**
     * 从查询串里摘掉 {@code token} 参数。
     * <p>
     * 回跳 URL 会被原样带进登录页的地址栏 —— 里面若留着 {@code ?token=<jwt>}，
     * 这枚 JWT 就又进了浏览器历史，还可能随登录页的外链进 Referer。
     * 登录成功后浏览器拿到的是新发的 Cookie，回跳串里那枚 token 没有任何用途，
     * 直接摘掉。（2026-10-21，TASK-20260924-017）
     */
    private static String stripTokenParam(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            String name = (eq >= 0) ? part.substring(0, eq) : part;
            if ("token".equalsIgnoreCase(name)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(part);
        }
        return sb.toString();
    }

    /**
     * 回跳地址的协议直接取 {@code sso.login-url} 的协议。解析不出（配成相对路径或畸形值）
     * 时退回请求自身的 scheme。
     */
    private String loginUrlScheme() {
        String loginUrl = ssoProperties.getLoginUrl();
        if (loginUrl != null) {
            int sep = loginUrl.indexOf("://");
            if (sep > 0) {
                return loginUrl.substring(0, sep).toLowerCase();
            }
        }
        return "http";
    }

    /**
     * 主机名取 {@code request.getServerName()} 而不是 {@code X-Forwarded-Host}：后者客户端可任意
     * 伪造，而回跳地址随后会被登录页拼上 token，等于开了一条把 token 送去任意站点的路。
     * Host 头则被网关的 vhost 分流约束着 —— 能路由到本服务，就说明它与本站的域名是对得上的。
     * <p>
     * 端口只在协议与请求自身一致时才用 {@code getServerPort()}：那是本地 dev 直连容器的形态，
     * 端口得带上。而协议一旦是被 {@code login-url} 换上去的，公网入口的端口就由协议决定
     * （https 即 443），容器的 80/8888 与公网端口无关 —— 拿它去比"是否默认端口"会把
     * {@code https://opc.zopc.top:80/...} 这种自相矛盾又必然连不通的地址拼出来。
     */
    private static String authority(HttpServletRequest request, String scheme) {
        String host = request.getServerName();
        if (!scheme.equalsIgnoreCase(request.getScheme())) {
            return host;
        }
        int port = request.getServerPort();
        return port == ("https".equalsIgnoreCase(scheme) ? 443 : 80)
                ? host : host + ":" + port;
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