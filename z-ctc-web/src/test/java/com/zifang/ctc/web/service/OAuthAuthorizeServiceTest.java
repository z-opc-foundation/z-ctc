package com.zifang.ctc.web.service;

import com.zifang.ctc.web.config.OAuthClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OAuth2 授权码 + PKCE 核心判据的单测 —— 无 Spring 容器、无数据库。
 *
 * <p>判据清单（每条都有正反例，客户端侧同一协议在 z-biz-tool-shared 有 30 例 node 单测）：
 * <ul>
 *   <li>client 未注册 → unauthorized_client；redirect 不在白名单 → invalid_request 且不允许回跳；</li>
 *   <li>response_type 只认 code；PKCE 强制 S256（plain / 缺失 / challenge 缺失都拒）；</li>
 *   <li>scope 必须是注册空间的子集，且必填；</li>
 *   <li>code 一次性（consume 第二次返回 null）；</li>
 *   <li>换 token 的四元组绑定：client / redirect / verifier 逐项对不上都拒，且统一 invalid_grant
 *       （不区分「过期/用过/伪造」—— 区分了等于给攻击者确认 code 曾有效）；</li>
 *   <li>PRI S256 判定与 RFC 7636 附录 B 的测试向量对齐（金标，不是自证）；</li>
 *   <li>loopback 通配端口：host/path 严格、端口任意；非 loopback 形态不许借通配混入。</li>
 * </ul>
 */
class OAuthAuthorizeServiceTest {

    private OAuthClientProperties props;
    private OAuthAuthorizeService service;

    private static final String CLIENT_ID = "z-biz-tool-remote";
    private static final String REDIRECT = "http://127.0.0.1:17888/callback";
    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    /** 与 CHALLENGE 配对的 verifier —— RFC 7636 附录 B 的官方测试向量。 */
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    @BeforeEach
    void setUp() {
        props = new OAuthClientProperties();
        OAuthClientProperties.Client c = new OAuthClientProperties.Client();
        c.setClientId(CLIENT_ID);
        c.setRedirectUris(java.util.List.of(
                REDIRECT,
                "http://127.0.0.1:*/callback"   // loopback 任意端口形态
        ));
        c.setScope("openid profile email");
        props.setClients(java.util.List.of(c));
        service = new OAuthAuthorizeService(props, new OAuthCodeStore());
    }

    /* ---------------- 授权端点判据 ---------------- */

    @Test
    @DisplayName("合法请求 → null（放行）")
    void authorizeHappyPath() {
        assertNull(service.validateAuthorize(CLIENT_ID, REDIRECT, "code",
                "openid profile email", "S256", CHALLENGE));
    }

    @Test
    @DisplayName("client 未注册 → unauthorized_client")
    void unknownClient() {
        assertEquals("unauthorized_client", service.validateAuthorize(
                "evil-app", REDIRECT, "code", "openid profile email", "S256", CHALLENGE));
    }

    @Test
    @DisplayName("redirect 不在白名单 → invalid_request（且不允许回跳）")
    void redirectNotRegistered() {
        assertEquals("invalid_request", service.validateAuthorize(CLIENT_ID,
                "http://127.0.0.1:17888/callback.evil.com", "code",
                "openid profile email", "S256", CHALLENGE));
        assertEquals("invalid_request", service.validateAuthorize(CLIENT_ID,
                "http://evil.com/callback", "code", "openid profile email", "S256", CHALLENGE));
    }

    @Test
    @DisplayName("redirect 前缀相似也不行（精确匹配，不是前缀匹配）")
    void redirectPrefixTrick() {
        assertEquals("invalid_request", service.validateAuthorize(CLIENT_ID,
                REDIRECT + ".evil.com", "code", "openid profile email", "S256", CHALLENGE));
    }

    @Test
    @DisplayName("response_type 只认 code")
    void responseTypeOnlyCode() {
        assertEquals("unsupported_response_type", service.validateAuthorize(
                CLIENT_ID, REDIRECT, "token", "openid profile email", "S256", CHALLENGE));
    }

    @Test
    @DisplayName("PKCE 缺 challenge / 用 plain / method 缺失 → 全拒")
    void pkceEnforced() {
        assertEquals("invalid_request", service.validateAuthorize(
                CLIENT_ID, REDIRECT, "code", "openid profile email", "S256", null));
        assertEquals("invalid_request", service.validateAuthorize(
                CLIENT_ID, REDIRECT, "code", "openid profile email", "plain", CHALLENGE));
        assertEquals("invalid_request", service.validateAuthorize(
                CLIENT_ID, REDIRECT, "code", "openid profile email", null, CHALLENGE));
    }

    @Test
    @DisplayName("scope 超出注册空间 / 缺失 → invalid_scope")
    void scopeEnforced() {
        assertEquals("invalid_scope", service.validateAuthorize(
                CLIENT_ID, REDIRECT, "code", "openid admin", "S256", CHALLENGE));
        assertEquals("invalid_scope", service.validateAuthorize(
                CLIENT_ID, REDIRECT, "code", null, "S256", CHALLENGE));
        // 子集放行
        assertNull(service.validateAuthorize(
                CLIENT_ID, REDIRECT, "code", "openid email", "S256", CHALLENGE));
    }

    /* ---------------- loopback 通配端口 ---------------- */

    @Test
    @DisplayName("loopback 通配：任意端口 + 同 host/path 放行")
    void loopbackWildcardAnyPort() {
        assertTrue(service.redirectAllowedPublic(props.find(CLIENT_ID),
                "http://127.0.0.1:52498/callback"));
        assertTrue(service.redirectAllowedPublic(props.find(CLIENT_ID),
                "http://127.0.0.1/callback"));
    }

    @Test
    @DisplayName("loopback 通配：host/path 不许变（通配只放开端口位）")
    void loopbackWildcardHostStrict() {
        // host 换成 localhost：语义上等价，但**字符上不等价** —— 严格匹配防开放式混入
        assertNotEquals(true, service.redirectAllowedPublic(props.find(CLIENT_ID),
                "http://localhost:52498/callback"));
        // path 变了不行
        assertNotEquals(true, service.redirectAllowedPublic(props.find(CLIENT_ID),
                "http://127.0.0.1:52498/callback/x"));
        // https 换 scheme 不行
        assertNotEquals(true, service.redirectAllowedPublic(props.find(CLIENT_ID),
                "https://127.0.0.1:52498/callback"));
    }

    /* ---------------- code 一次性 ---------------- */

    @Test
    @DisplayName("同一条 code 第二次消费 → null（一次性）")
    void codeSingleUseSameStore() {
        OAuthCodeStore store = new OAuthCodeStore();
        OAuthAuthorizeService svc = new OAuthAuthorizeService(props, store);
        String code = svc.issueCode(CLIENT_ID, REDIRECT, CHALLENGE, "u_1", "openid profile email");

        Object first = svc.validateTokenRequest("authorization_code", code, CLIENT_ID, REDIRECT, VERIFIER);
        assertTrue(first instanceof OAuthAuthorizeService.TokenGrant, "第一次应成功: " + first);

        Object second = svc.validateTokenRequest("authorization_code", code, CLIENT_ID, REDIRECT, VERIFIER);
        assertEquals("invalid_grant", second, "第二次必须拒绝（一次性）");
    }

    /* ---------------- 换 token：四元组绑定 ---------------- */

    @Test
    @DisplayName("grant_type 不是 authorization_code → unsupported_grant_type")
    void grantTypeOnlyAuthCode() {
        assertEquals("unsupported_grant_type",
                service.validateTokenRequest("password", "c", CLIENT_ID, REDIRECT, "v"));
    }

    @Test
    @DisplayName("不存在的 code → invalid_grant（不区分过期/伪造）")
    void unknownCode() {
        assertEquals("invalid_grant",
                service.validateTokenRequest("authorization_code", "no-such-code", CLIENT_ID, REDIRECT, VERIFIER));
    }

    @Test
    @DisplayName("client 不匹配 / redirect 不匹配 → invalid_grant")
    void codeBoundToClientAndRedirect() {
        String code = service.issueCode(CLIENT_ID, REDIRECT, CHALLENGE, "u_1", "openid profile email");
        assertEquals("invalid_grant", service.validateTokenRequest(
                "authorization_code", code, "z-biz-tool-mind", REDIRECT, VERIFIER));
        assertEquals("invalid_grant", service.validateTokenRequest(
                "authorization_code", code, CLIENT_ID, "http://evil.com/callback", VERIFIER));
    }

    @Test
    @DisplayName("PKCE verifier 对不上 → invalid_grant（金标向量）")
    void pkceVerifierMismatch() {
        // verifier 检查在 consume 之后 ⇒ 每条断言用**独立**的 code，
        // 否则第一条断言就把 code 消费掉，第二条拿到的是「code 已不存在」的错。
        String code1 = service.issueCode(CLIENT_ID, REDIRECT, CHALLENGE, "u_1", "openid profile email");
        assertEquals("invalid_grant", service.validateTokenRequest(
                "authorization_code", code1, CLIENT_ID, REDIRECT, "wrong-verifier"));
        String code2 = service.issueCode(CLIENT_ID, REDIRECT, CHALLENGE, "u_1", "openid profile email");
        assertEquals("invalid_request", service.validateTokenRequest(
                "authorization_code", code2, CLIENT_ID, REDIRECT, null));
    }

    @Test
    @DisplayName("全对 → TokenGrant（user 与 scope 原样带回）")
    void tokenHappyPath() {
        String code = service.issueCode(CLIENT_ID, REDIRECT, CHALLENGE, "u_42", "openid email");
        Object result = service.validateTokenRequest(
                "authorization_code", code, CLIENT_ID, REDIRECT, VERIFIER);
        assertTrue(result instanceof OAuthAuthorizeService.TokenGrant, "应成功: " + result);
        OAuthAuthorizeService.TokenGrant grant = (OAuthAuthorizeService.TokenGrant) result;
        assertEquals("u_42", grant.userId);
        assertEquals("openid email", grant.scope);
    }

    /* ---------------- PKCE S256 金标（RFC 7636 附录 B） ---------------- */

    @Test
    @DisplayName("S256 与 RFC 7636 附录 B 测试向量逐字节一致")
    void s256RfcVector() {
        assertEquals(CHALLENGE, OAuthAuthorizeService.s256Base64Url(VERIFIER));
    }

    @Test
    @DisplayName("随机 code 有足够长度与唯一性")
    void randomTokenShape() {
        String a = service.randomToken();
        String b = service.randomToken();
        assertEquals(22, a.length()); // 128-bit → base64url 无填充 = 22 字符
        assertNotEquals(a, b);
    }
}
