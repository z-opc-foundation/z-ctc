package com.zifang.ctc.web.service;

import com.zifang.ctc.web.config.OAuthClientProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * OAuth2 授权码 + PKCE 的核心判据 —— <b>协议逻辑唯一所在地</b>。
 *
 * <p>Controller 只做参数搬运与 HTTP 形状；这里做全部安全判定。
 * 集中成一个无 HTTP 依赖的服务，是为了让 Mockito 能直接逐条钉判据
 * （家族客户端侧同一套协议有 30 例 node 单测，见 z-biz-tool-shared/src/sso/oauth.ts ——
 * 两端各测各的实现，契约形状一致）。
 *
 * <p><b>三条安全基线（缺一即是洞）</b>：
 * <ol>
 *   <li><b>PRI 强制 S256</b>：授权请求必须带 {@code code_challenge_method=S256}；
 *       {@code plain} 不收（challenge=verifier 等于没有保护），缺失也不收。</li>
 *   <li><b>redirect_uri 精确匹配</b>登记白名单（loopback 通配端口特例见
 *       {@link OAuthClientProperties}）—— 前缀匹配会被
 *       {@code .../callback.evil.com} 骗过。</li>
 *   <li><b>code 一次性 + 5 分钟 + 四元组绑定</b>（见 {@link OAuthCodeStore}）。</li>
 * </ol>
 *
 * <p><b>错误码形状对齐 RFC 6749 §5.2</b>：{@code invalid_request / invalid_client /
 * invalid_grant / unsupported_grant_type / access_denied} —— 家族客户端的
 * {@code describeTokenError} 就按这个形状解析展示。
 */
@Service
public class OAuthAuthorizeService {

    private static final Logger log = LogManager.getLogger(OAuthAuthorizeService.class);

    /** 授权码有效期（秒）—— 与 {@link OAuthCodeStore} 的 TTL 一致。 */
    public static final long CODE_TTL_SECONDS = 5 * 60;

    /** access_token 有效期（秒）—— 与 ctc 登录 token 的 8 小时一致。 */
    public static final long ACCESS_TOKEN_TTL_SECONDS = 8 * 60 * 60;

    private final OAuthClientProperties clients;
    private final OAuthCodeStore codeStore;

    /** code / 随机源。SecureRandom 无种子注入 —— 不确定性是它存在的意义。 */
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public OAuthAuthorizeService(OAuthClientProperties clients, OAuthCodeStore codeStore) {
        this.clients = clients;
        this.codeStore = codeStore;
    }

    /* ------------------------------------------------------------------ *
     * 授权端点判据
     * ------------------------------------------------------------------ */

    /**
     * 校验授权请求。全部通过返回 null，否则返回 RFC 错误码。
     *
     * @param clientId        请求里的 client_id
     * @param redirectUri     请求里的 redirect_uri（编码前的原值）
     * @param responseType    请求里的 response_type（只认 "code"）
     * @param scope           请求里的 scope（必须是注册 scope 的子集）
     * @param challengeMethod 请求里的 code_challenge_method
     * @param codeChallenge   请求里的 code_challenge
     */
    public String validateAuthorize(String clientId, String redirectUri, String responseType,
                                    String scope, String challengeMethod, String codeChallenge) {
        OAuthClientProperties.Client client = clients.find(clientId);
        if (client == null) {
            return "unauthorized_client";
        }
        if (!redirectAllowed(client, redirectUri)) {
            // redirect 不匹配时**绝不**带着 redirect 跳错误 —— 防开放重定向；
            // 调用方必须渲染错误页而不是 302 回这个地址。
            return "invalid_request";
        }
        if (!"code".equals(responseType)) {
            // response_type 错误可以带 redirect（RFC 6749 §4.1.2.1）—— 地址本身已验证。
            return "unsupported_response_type";
        }
        if (codeChallenge == null || codeChallenge.isEmpty()) {
            return "invalid_request";
        }
        if (!"S256".equals(challengeMethod)) {
            // plain 与缺失一律拒：plain 的 challenge 就是 verifier，等于裸奔。
            return "invalid_request";
        }
        if (!scopeAllowed(client, scope)) {
            return "invalid_scope";
        }
        return null;
    }

    /** Controller 判「能不能安全回跳」的公开门面（redirect 不合法时绝不 302）。 */
    public boolean redirectAllowedPublic(OAuthClientProperties.Client client, String redirectUri) {
        return client != null && redirectAllowed(client, redirectUri);
    }

    /** redirect 白名单判定。 */
    boolean redirectAllowed(OAuthClientProperties.Client client, String redirectUri) {

        if (redirectUri == null || redirectUri.isEmpty()) {
            return false;
        }
        for (String registered : client.getRedirectUris()) {
            if (registered == null || registered.isEmpty()) {
                continue;
            }
            // 通配端口的 loopback 形态：http://127.0.0.1:*/callback
            if (registered.contains(":*/")) {
                String regNoPort = registered.replace(":*", "");
                String reqNoPort = stripPort(redirectUri);
                if (regNoPort.equals(reqNoPort)) {
                    return true;
                }
                continue;
            }
            // 精确匹配（其余一切形态）
            if (registered.equals(redirectUri)) {
                return true;
            }
        }
        return false;
    }

    /** 去掉 URL 里的端口段（scheme://host:port/path → scheme://host/path）。 */
    private static String stripPort(String uri) {
        int schemeEnd = uri.indexOf("://");
        if (schemeEnd < 0) {
            return uri;
        }
        int hostStart = schemeEnd + 3;
        int slash = uri.indexOf('/', hostStart);
        if (slash < 0) {
            return uri;
        }
        String hostPort = uri.substring(hostStart, slash);
        int colon = hostPort.indexOf(':');
        if (colon < 0) {
            return uri; // 本来就没端口
        }
        return uri.substring(0, hostStart) + hostPort.substring(0, colon) + uri.substring(slash);
    }

    /** scope 判定：请求 scope 的每一项都必须在注册 scope 空间里。 */
    boolean scopeAllowed(OAuthClientProperties.Client client, String requested) {
        if (requested == null || requested.trim().isEmpty()) {
            return false; // scope 必填 —— 家族客户端固定带 openid profile email
        }
        Set<String> allowed = splitScope(client.getScope());
        if (allowed.isEmpty()) {
            return false;
        }
        return splitScope(requested).stream().allMatch(allowed::contains);
    }

    private static Set<String> splitScope(String s) {
        Set<String> out = new LinkedHashSet<>();
        if (s != null) {
            for (String item : s.trim().split("\\s+")) {
                if (!item.isEmpty()) {
                    out.add(item);
                }
            }
        }
        return out;
    }

    /**
     * 签发授权码并落库（四元组绑定）。
     *
     * @return 新的授权码（128-bit 随机 base64url）
     */
    public String issueCode(String clientId, String redirectUri, String codeChallenge,
                            String userId, String scope) {
        String code = randomToken();
        codeStore.issue(code, new OAuthCodeStore.CodeEntry(clientId, redirectUri, codeChallenge, userId, scope));
        return code;
    }

    /* ------------------------------------------------------------------ *
     * 令牌端点判据
     * ------------------------------------------------------------------ */

    /**
     * 校验换 token 请求（授权码模式）。
     * 通过返回 {@link TokenGrant}，否则返回 RFC 错误码（由 controller 转 HTTP 400）。
     */
    public Object validateTokenRequest(String grantType, String code, String clientId,
                                       String redirectUri, String codeVerifier) {
        if (!"authorization_code".equals(grantType)) {
            return "unsupported_grant_type";
        }
        if (code == null || code.isEmpty() || clientId == null || clientId.isEmpty()) {
            return "invalid_request";
        }
        OAuthCodeStore.CodeEntry entry = codeStore.consume(code);
        if (entry == null) {
            // 不存在 = 过期 / 用过 / 伪造。三者统一按 invalid_grant 拒（不区分 ——
            // 区分了等于给攻击者确认「这条 code 曾经有效」）。
            return "invalid_grant";
        }
        // 四元组逐项绑定校验
        if (!entry.clientId.equals(clientId)) {
            log.warn("OAuth token: code 的 client 不匹配 (expect={}, got={})", entry.clientId, clientId);
            return "invalid_grant";
        }
        if (!entry.redirectUri.equals(redirectUri == null ? "" : redirectUri)) {
            log.warn("OAuth token: code 的 redirect_uri 不匹配 (client={})", clientId);
            return "invalid_grant";
        }
        if (codeVerifier == null || codeVerifier.isEmpty()) {
            return "invalid_request";
        }
        // PKCE：S256(verifier) == 当初授权请求里的 challenge
        String computed = s256Base64Url(codeVerifier);
        if (!MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.US_ASCII),
                entry.codeChallenge.getBytes(StandardCharsets.US_ASCII))) {
            log.warn("OAuth token: PKCE verifier 不匹配 (client={})", clientId);
            return "invalid_grant";
        }
        return new TokenGrant(entry.userId, entry.scope);
    }

    /** 换 token 成功后的载荷。 */
    public static class TokenGrant {
        public final String userId;
        public final String scope;

        public TokenGrant(String userId, String scope) {
            this.userId = userId;
            this.scope = scope;
        }
    }

    /* ------------------------------------------------------------------ *
     * 工具
     * ------------------------------------------------------------------ */

    /** 128-bit 随机 → base64url（无填充）。code 与 access_token 共用这个强度。 */
    public String randomToken() {
        byte[] buf = new byte[16];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /** PKCE 的 S256：SHA-256(verifier) → base64url（无填充）。RFC 7636 §4.2 的原话。 */
    public static String s256Base64Url(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            // JVM 规范强制自带 SHA-256 —— 真发生说明 JVM 坏了，按运行时错误抛。
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
