package com.zifang.ctc.web.api;

import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.mapper.AccountMapper;
import com.zifang.ctc.core.domain.mapper.LoginLogMapper;
import com.zifang.ctc.core.domain.mapper.MembershipMapper;
import com.zifang.ctc.core.domain.mapper.UserMapper;
import com.zifang.ctc.core.service.AuthService;
import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.config.SsoProperties;
import com.zifang.ctc.web.api.request.SwitchTenantRequest;
import com.zifang.ctc.web.api.response.MyTenantsResult;
import com.zifang.util.core.meta.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 租户端点（my-tenants / select-tenant）的**凭证来源**守卫（2026-11-09「未登录或 token 缺少 userId」）。
 *
 * <p>病根（改前）: 这些端点在 {@code sso.exclude-paths} 里自解析身份, 但
 * {@code extractUserId} 只认 {@code Authorization: Bearer} 头; 而 z-team 顶栏切租户 /
 * onboarding 走原生 fetch {@code credentials:include} —— 只带 SSO Cookie 不带头
 * ⇒ 已登录的 Cookie 会话被判成 401「未登录或 token 缺少 userId」
 * （whoami 的同款问题已在 FEATURE-ZTEAM-AUTH-UNIFY 修过）。</p>
 *
 * <p>本测试钉住三件事：</p>
 * <ol>
 *   <li><b>Cookie 会话可用</b>: 只带 SSO Cookie 的请求, my-tenants / select-tenant 正常返回；</li>
 *   <li><b>无任何凭证仍 401</b>: 兜底语义不许放松；</li>
 *   <li><b>Authorization 头仍优先</b>: 带头调用方（axios authRequest）行为不变。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class AuthnControllerTenantCookieSessionTest {

    private static final String COOKIE_NAME = "sso_token";
    private static final String COOKIE_TOKEN = "cookie-jwt-token";
    private static final String HEADER_TOKEN = "header-jwt-token";

    @Mock
    private AuthService authService;
    @Mock
    private LoginLogMapper loginLogMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private SsoProperties ssoProperties;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MembershipMapper membershipMapper;

    @InjectMocks
    private AuthnController controller;

    @BeforeEach
    void injectDependencies() {
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(controller, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(controller, "ssoProperties", ssoProperties);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "membershipMapper", membershipMapper);
    }

    // ------------------------------------------------------------------
    // ① Cookie 会话可用
    // ------------------------------------------------------------------

    @Test
    @DisplayName("只带 SSO Cookie 的 my-tenants 不再 401（改前只认 Authorization 头）")
    void myTenantsResolvesUserIdFromCookie() {
        when(ssoProperties.getEffectiveTokenCookieNames()).thenReturn(List.of(COOKIE_NAME));
        when(jwtUtil.verifyToken(COOKIE_TOKEN)).thenReturn(validClaims(7L));
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(membershipMapper.selectActiveByUserId(7L)).thenReturn(Collections.emptyList());

        Result<MyTenantsResult> r = controller.myTenants(cookieOnlyRequest());

        assertTrue(r.isSuccess(), "Cookie 会话应被视为已登录, 实际: " + r.getMessage());
        assertEquals(7L, r.getData().getUserId());
        assertEquals(0, r.getData().getTenantCount());
        assertTrue(r.getData().isOrphan());
        verify(userMapper).selectById(7L);
    }

    @Test
    @DisplayName("只带 SSO Cookie 的 select-tenant 走通签发（顶栏切租户链路）")
    void selectTenantResolvesUserIdFromCookie() {
        when(ssoProperties.getEffectiveTokenCookieNames()).thenReturn(List.of(COOKIE_NAME));
        when(jwtUtil.verifyToken(COOKIE_TOKEN)).thenReturn(validClaims(7L));
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(membershipMapper.selectByUserAndTenant(7L, "acme")).thenReturn(activeMembership(7L, "acme"));
        when(accountMapper.selectById(7L)).thenReturn(account(7L));
        when(jwtUtil.generateToken(anyMap(), anyLong())).thenReturn("new-token");
        when(ssoProperties.getTokenCookieName()).thenReturn(COOKIE_NAME);

        SwitchTenantRequest req = new SwitchTenantRequest();
        req.setTenantCode("acme");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        Result<com.zifang.ctc.core.vo.LoginResponseVO> r = controller.selectTenant(req, cookieOnlyRequest(), resp);

        assertTrue(r.isSuccess(), "Cookie 会话切租户应成功, 实际: " + r.getMessage());
        assertEquals("new-token", r.getData().getToken());
        verify(jwtUtil).generateToken(anyMap(), eq(8 * 60 * 60L));
        // 切换必须同步刷新 SSO cookie (否则浏览器 cookie 还是旧 claim, whoami 会把 domainCode 抹回 null)
        verify(resp).addHeader(eq("Set-Cookie"), startsWith("sso_token=new-token"));
    }

    @Test
    @DisplayName("Cookie 里的 token 校验不过 ⇒ 仍 401（兜底不放松）")
    void invalidCookieTokenIsStillUnauthorized() {
        when(ssoProperties.getEffectiveTokenCookieNames()).thenReturn(List.of(COOKIE_NAME));
        when(jwtUtil.verifyToken(COOKIE_TOKEN))
                .thenReturn(new JwtUtil.VerificationResult(false, null, "token 过期"));

        Result<MyTenantsResult> r = controller.myTenants(cookieOnlyRequest());

        assertEquals(401, r.getCode());
        // Result.fail 自带「处理失败|」前缀, 只断言稳定的信息主体
        assertTrue(r.getMessage().contains("未登录或 token 缺少 userId"));
    }

    // ------------------------------------------------------------------
    // ② 无任何凭证仍 401
    // ------------------------------------------------------------------

    @Test
    @DisplayName("头 / Cookie 都没有 ⇒ 401 未登录或 token 缺少 userId")
    void anonymousRequestIsStillUnauthorized() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        Result<MyTenantsResult> r = controller.myTenants(request);

        assertEquals(401, r.getCode());
        assertTrue(r.getMessage().contains("未登录或 token 缺少 userId"));
        verify(jwtUtil, never()).verifyToken(anyString());
    }

    // ------------------------------------------------------------------
    // ③ Authorization 头仍优先
    // ------------------------------------------------------------------

    @Test
    @DisplayName("带头时用头的 token, 不再碰 Cookie（axios authRequest 调用方语义不变）")
    void headerTokenTakesPrecedenceOverCookie() {
        when(jwtUtil.verifyToken(HEADER_TOKEN)).thenReturn(validClaims(8L));
        when(userMapper.selectById(8L)).thenReturn(user(8L));
        when(membershipMapper.selectActiveByUserId(8L)).thenReturn(Collections.emptyList());

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + HEADER_TOKEN);
        // 头优先 ⇒ Cookie 不会被读取; lenient 免得 strict-stub 判未消费
        lenient().when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(COOKIE_NAME, COOKIE_TOKEN)});

        Result<MyTenantsResult> r = controller.myTenants(request);

        assertTrue(r.isSuccess(), "带头会话应照常工作, 实际: " + r.getMessage());
        assertEquals(8L, r.getData().getUserId());
        verify(jwtUtil, never()).verifyToken(COOKIE_TOKEN);
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static HttpServletRequest cookieOnlyRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(COOKIE_NAME, COOKIE_TOKEN)});
        return request;
    }

    private static JwtUtil.VerificationResult validClaims(Long userId) {
        return new JwtUtil.VerificationResult(true, Map.of("userId", String.valueOf(userId)), null);
    }

    private static UserDO user(Long id) {
        UserDO u = new UserDO();
        u.setId(id);
        u.setUserNo("U" + id);
        u.setUsername("u" + id);
        u.setNickname("u" + id);
        u.setEmail("u" + id + "@example.com");
        u.setStatus(1);
        return u;
    }

    private static MembershipDO activeMembership(Long userId, String tenantCode) {
        MembershipDO m = new MembershipDO();
        m.setId(1L);
        m.setUserId(userId);
        m.setTenantCode(tenantCode);
        m.setRoleCode("owner");
        m.setMemberStatus(1);
        return m;
    }

    private static AccountDO account(Long id) {
        AccountDO a = new AccountDO();
        a.setId(id);
        a.setAccountNo("U" + id);
        a.setUsername("u" + id);
        a.setNickname("u" + id);
        a.setStatus(1);
        a.setAccountType(2);
        return a;
    }
}
