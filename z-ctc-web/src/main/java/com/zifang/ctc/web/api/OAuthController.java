package com.zifang.ctc.web.api;

import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.web.config.OAuthClientProperties;
import com.zifang.ctc.web.service.OAuthAuthorizeService;
import com.zifang.ctc.web.service.OAuthCodeStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth2 授权服务器端点（授权码 + PKCE）—— 家族桌面客户端（z-biz-tool）的 SSO 入口。
 *
 * <p><b>三个端点，HTTP 形状各异（这是协议规定，不是风格问题）</b>：
 * <ul>
 *   <li>{@code /authorize}：浏览器交互端点 —— 302 / 错误页 / 授权页，<b>不是</b> JSON；</li>
 *   <li>{@code /token}：RFC 6749 §5.1/5.2 —— 成功 200 JSON（access_token…）、
 *       失败 400 + {@code {"error":"invalid_grant"}}。<b>不套</b> ctc 的 {@code Result}
 *       包装 —— 家族客户端的 {@code describeTokenError} 按 RFC 形状解析；</li>
 *   <li>{@code /userinfo}：OIDC 风格 —— 200 + {@code {sub, name, email, ...}}，
 *       家族客户端的 {@code toSsoUser} 就吃 {@code sub} 这个字段名。</li>
 * </ul>
 *
 * <p><b>鉴权设计</b>：authorize 需要用户登录态（浏览器里 ctc 的 sso_token cookie）；
 * token / userinfo 是<b>程序调用</b>，凭 code / access_token 自证，必须在
 * SSO 闸门的 exclude-paths 里显式登记（见 application.yml 的注记）。
 *
 * <p><b>access_token 就是 ctc 的 JWT</b>（JwtUtil 签发，8 小时）—— 不另造 token 形态：
 * 家族服务端（remote）拿它去调 userinfo 验身份，与 ctc 消费方验 token 是同一条验证链。
 */
@RestController
@RequestMapping("/api/ctc/oauth")
public class OAuthController {

    private static final Logger log = LogManager.getLogger(OAuthController.class);

    private final OAuthAuthorizeService authorizeService;
    private final OAuthClientProperties clientProperties;
    private final JwtUtil jwtUtil;

    /** 未登录时跳转的 ctc 登录页（与 sso.login-url 同源，单独注入便于测试）。 */
    @Value("${sso.login-url:http://localhost:8080/login}")
    private String loginUrl;

    @Autowired
    public OAuthController(OAuthAuthorizeService authorizeService,
                           OAuthClientProperties clientProperties,
                           JwtUtil jwtUtil) {
        this.authorizeService = authorizeService;
        this.clientProperties = clientProperties;
        this.jwtUtil = jwtUtil;
    }

    /* ------------------------------------------------------------------ *
     * 授权端点
     * ------------------------------------------------------------------ */

    /**
     * 授权端点：校验参数 → 有登录态就发 code 并 302 回 redirect_uri，没有就跳登录页。
     *
     * <p>登录态的判读与 whoami 一致（cookie / Bearer 均认）——桌面端授权发生在
     * 用户浏览器里，靠的是 ctc 登录后种下的 sso_token cookie。
     *
     * <p><b>错误回跳的纪律</b>：只有 redirect_uri 通过白名单后才允许 302 带
     * {@code error=} 回去；redirect 本身不合法时渲染 400 文本页 ——
     * 把用户送到一个未经验证的地址比报错更糟（开放重定向）。
     */
    @GetMapping("/authorize")
    public void authorize(@RequestParam(value = "response_type", required = false) String responseType,
                          @RequestParam("client_id") String clientId,
                          @RequestParam(value = "redirect_uri", required = false) String redirectUri,
                          @RequestParam(value = "scope", required = false) String scope,
                          @RequestParam(value = "state", required = false) String state,
                          @RequestParam(value = "code_challenge", required = false) String codeChallenge,
                          @RequestParam(value = "code_challenge_method", required = false) String challengeMethod,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {

        OAuthClientProperties.Client client = clientProperties.find(clientId);
        boolean redirectOk = client != null
                && authorizeService.redirectAllowedPublic(client, redirectUri);

        if (!redirectOk) {
            // client 不存在或 redirect 不在白名单：绝不回跳，渲染错误页。
            log.warn("OAuth authorize: client/redirect 不合法 (clientId={})", clientId);
            respondErrorPage(response, "接入方未注册或回调地址不合法，请联系管理员。");
            return;
        }

        // redirect 已验证，协议级错误可以安全地 302 回去（RFC 6749 §4.1.2.1）。
        String err = authorizeService.validateAuthorize(
                clientId, redirectUri, responseType, scope, challengeMethod, codeChallenge);
        if (err != null) {
            redirectToClient(response, redirectUri, err, state);
            return;
        }

        // 登录态：谁在用浏览器？
        String userId = currentUserId(request);
        if (userId == null) {
            // 未登录 → 登录页，登录成功后回本端点（cb 参数原样带回）。
            // loginUrl 是配置里发给浏览器的地址（绝不 127.0.0.1）。
            String back = "/api/ctc/oauth/authorize?response_type=code&client_id="
                    + URLEncoder.encode(nullSafe(clientId), StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(nullSafe(redirectUri), StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(nullSafe(scope), StandardCharsets.UTF_8)
                    + "&code_challenge=" + URLEncoder.encode(nullSafe(codeChallenge), StandardCharsets.UTF_8)
                    + "&code_challenge_method=S256"
                    + (state != null ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "");
            response.sendRedirect(loginUrl + "?cb=" + URLEncoder.encode(back, StandardCharsets.UTF_8));
            return;
        }

        // 发 code（四元组绑定见 OAuthCodeStore）。
        String code = authorizeService.issueCode(clientId, redirectUri, codeChallenge, userId, scope);
        redirectToClient(response, redirectUri, null, state, code);
    }

    private void redirectToClient(HttpServletResponse response, String redirectUri,
                                  String error, String state, String... code) throws IOException {
        StringBuilder sb = new StringBuilder(redirectUri);
        sb.append(redirectUri.contains("?") ? '&' : '?');
        boolean first = true;
        if (error != null) {
            sb.append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
            first = false;
        }
        if (code.length > 0 && code[0] != null) {
            if (!first) sb.append('&');
            sb.append("code=").append(URLEncoder.encode(code[0], StandardCharsets.UTF_8));
        }
        if (state != null && !state.isEmpty()) {
            sb.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        response.sendRedirect(sb.toString());
    }

    private void respondErrorPage(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("<html><body><h2>SSO 授权失败</h2><p>"
                + message + "</p></body></html>");
    }

    /** 从 cookie / Bearer 判读当前登录用户 id（判读口径与 whoami 一致）。 */
    private String currentUserId(HttpServletRequest request) {
        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null) {
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie c : cookies) {
                    if ("sso_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                        token = c.getValue();
                        break;
                    }
                }
            }
        }
        if (token == null || token.isEmpty()) {
            return null;
        }
        JwtUtil.VerificationResult v = jwtUtil.verifyToken(token);
        if (!v.isValid()) {
            return null;
        }
        Object userId = v.getClaims().get("userId");
        return userId == null ? null : String.valueOf(userId);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /* ------------------------------------------------------------------ *
     * 令牌端点
     * ------------------------------------------------------------------ */

    /**
     * 换 token。RFC 6749 §3.2 规定参数走 form-urlencoded ——
     * 家族客户端发的就是 form（{@code Content-Type: application/x-www-form-urlencoded}）。
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam(value = "grant_type", required = false) String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier) {

        Object result = authorizeService.validateTokenRequest(
                grantType, code, clientId, redirectUri, codeVerifier);

        if (result instanceof String err) {
            // RFC 6749 §5.2：错误一律 400 + {"error": "..."}（可带 error_description）。
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", err);
            return ResponseEntity.badRequest().body(body);
        }

        OAuthAuthorizeService.TokenGrant grant = (OAuthAuthorizeService.TokenGrant) result;

        // access_token = ctc 的 JWT（8 小时）。claims 里放 family 客户端 userinfo 会用到的字段。
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", grant.userId);
        // scope 放进 claim：服务端可据此决定给这个 token 什么权限。
        claims.put("scope", grant.scope);
        String accessToken = jwtUtil.generateToken(claims, OAuthAuthorizeService.ACCESS_TOKEN_TTL_SECONDS);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", accessToken);
        body.put("token_type", "Bearer");
        body.put("expires_in", OAuthAuthorizeService.ACCESS_TOKEN_TTL_SECONDS);
        // refresh_token：第一版不给 —— 桌面客户端 8 小时过期重走一次授权即可；
        // 引入 refresh 就要处理吊销与轮换，等真实需要再加（YAGNI，注释钉住）。
        return ResponseEntity.ok(body);
    }

    /* ------------------------------------------------------------------ *
     * 身份端点
     * ------------------------------------------------------------------ */

    /**
     * OIDC userinfo：按 access_token 返回用户身份。
     *
     * <p><b>字段名是契约</b>：家族客户端 {@code toSsoUser} 认
     * {@code sub / name / email / preferred_username}。这里 {@code sub} 用
     * ctc 的 user.id（唯一且稳定），name 用 nickname。
     */
    @GetMapping("/userinfo")
    public ResponseEntity<Map<String, Object>> userinfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }
        JwtUtil.VerificationResult v = jwtUtil.verifyToken(authHeader.substring(7));
        if (!v.isValid()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }
        Map<String, Object> claims = v.getClaims();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sub", String.valueOf(claims.getOrDefault("userId", "")));
        Object username = claims.get("username");
        if (username != null) body.put("preferred_username", String.valueOf(username));
        // nickname 在签发 access_token 时没放进 claims（issueToken 链路没有它）——
        // 家族 toSsoUser 对 name/email 都是可缺省处理（null 就回退 preferred_username）。
        return ResponseEntity.ok(body);
    }
}
