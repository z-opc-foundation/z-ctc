package com.zifang.ctc.web.api;

import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.mapper.AccountMapper;
import com.zifang.ctc.core.domain.mapper.AuthMapper;
import com.zifang.ctc.core.domain.mapper.LoginLogMapper;
import com.zifang.ctc.core.domain.mapper.MembershipMapper;
import com.zifang.ctc.core.domain.mapper.UserMapper;
import com.zifang.ctc.core.service.AccountService;
import com.zifang.ctc.core.service.AuthService;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.core.service.UserService;
import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.config.SsoProperties;
import com.zifang.ctc.web.api.response.RegisterResult;
import com.zifang.ctc.web.service.VerifyCodeService;
import com.zifang.util.core.meta.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 自助注册的**行为**守卫（卡 TASK-20261022-027 第 3、4 项）。
 *
 * <p>病根（改前）：{@code POST /api/ctc/authn/register} 只要 {@code email} + {@code password}(≥6 位)
 * 就能建号，**且当场签发一枚 SSO 闸门认可的 token** ⇒ 「匿名 → 零凭据 → 合法登录态 → 闸门被绕过」。</p>
 *
 * <p>本测试钉住两件事：</p>
 * <ol>
 *   <li><b>必须有邮箱验证码</b>：缺码 / 错码 ⇒ 拒绝，且<b>不得建号</b>；</li>
 *   <li><b>不得签发闸门认可的 token</b>：注册成功返回的 {@code token} 必须为空，
 *       且整个链路<b>一次都不许</b>调 {@code jwtUtil.generateToken}。</li>
 * </ol>
 *
 * <p>「豁免清单」那一侧的边界断言在
 * {@code z-ctc-admin/src/test/java/com/zifang/ctc/admin/authz/SsoExcludePathsBoundaryTest.java}。</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthnControllerSelfRegisterTest {

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
    @Mock
    private AccountService accountService;
    @Mock
    private UserService userService;
    @Mock
    private MembershipService membershipService;
    @Mock
    private AuthMapper authMapper;
    @Mock
    private VerifyCodeService verifyCodeService;

    @InjectMocks
    private AuthnController controller;

    /**
     * {@code AuthnController} 的依赖是 {@code @Autowired} **字段**注入 ⇒ Mockito 的
     * {@code @InjectMocks} 只会走构造器（{@code AuthService} + {@code LoginLogMapper}），
     * 其余字段实测全是 {@code null}（verifyCodeService / userService / jwtUtil …）
     * ⇒ 这里显式逐个注入，不依赖 Mockito 的注入策略。
     */
    @BeforeEach
    void injectDependencies() {
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(controller, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(controller, "ssoProperties", ssoProperties);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "membershipMapper", membershipMapper);
        ReflectionTestUtils.setField(controller, "accountService", accountService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "membershipService", membershipService);
        ReflectionTestUtils.setField(controller, "authMapper", authMapper);
        ReflectionTestUtils.setField(controller, "verifyCodeService", verifyCodeService);
    }

    private static final String EMAIL = "someone@example.com";
    private static final String PHONE = "13800000000";

    // ------------------------------------------------------------------
    // 第 3 项: 注册必须有验证码
    // ------------------------------------------------------------------

    @Test
    @DisplayName("注册缺验证码 ⇒ 400 且不建号（改前只要 email+password 就能建号）")
    void registerWithoutCodeIsRejectedAndCreatesNoUser() {
        AuthnController.RegisterRequest req = new AuthnController.RegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword("secret123");

        Result<RegisterResult> r = controller.register(req);

        assertEquals(400, r.getCode());
        assertNull(r.getData());
        verify(userService, never()).register(anyString(), any(), any(), anyString());
        verify(verifyCodeService, never()).verifyCode(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("注册验证码错误 ⇒ 400 且不建号")
    void registerWithWrongCodeIsRejectedAndCreatesNoUser() {
        doThrow(new IllegalArgumentException("验证码错误"))
                .when(verifyCodeService)
                .verifyCode(eq(EMAIL), eq(VerifyCodeService.CHANNEL_EMAIL),
                        eq(VerifyCodeService.SCENE_REGISTER), eq("000000"));

        AuthnController.RegisterRequest req = new AuthnController.RegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword("secret123");
        req.setCode("000000");

        Result<RegisterResult> r = controller.register(req);

        assertEquals(400, r.getCode());
        verify(userService, never()).register(anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("注册走的是 EMAIL + REGISTER 场景的验证码")
    void registerVerifiesEmailRegisterSceneCode() {
        AuthnController.RegisterRequest req = new AuthnController.RegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword("secret123");
        req.setCode("123456");
        when(userService.register(anyString(), any(), any(), anyString())).thenReturn(100L);
        when(userService.findById(100L)).thenReturn(Optional.of(user(100L, EMAIL)));

        controller.register(req);

        verify(verifyCodeService).verifyCode(eq(EMAIL), eq(VerifyCodeService.CHANNEL_EMAIL),
                eq(VerifyCodeService.SCENE_REGISTER), eq("123456"));
    }

    // ------------------------------------------------------------------
    // 第 4 项: 自助注册不得无条件签发「闸门认可」的 token
    // ------------------------------------------------------------------

    @Test
    @DisplayName("邮箱注册成功也不签发 token（闸门认可的登录态只能由正常登录产生）")
    void registerIssuesNoToken() {
        AuthnController.RegisterRequest req = new AuthnController.RegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword("secret123");
        req.setCode("123456");
        when(userService.register(anyString(), any(), any(), anyString())).thenReturn(100L);
        when(userService.findById(100L)).thenReturn(Optional.of(user(100L, EMAIL)));

        Result<RegisterResult> r = controller.register(req);

        assertTrue(r.isSuccess());
        assertNotNull(r.getData());
        assertNull(r.getData().getToken(), "自助注册不得签发 SSO 闸门认可的 token");
        assertTrue(r.getData().isOrphan());
        verify(jwtUtil, never()).generateToken(anyMap(), anyLong());
    }

    @Test
    @DisplayName("手机号注册成功也不签发 token")
    void registerPhoneIssuesNoToken() {
        AuthnController.PhoneRegisterRequest req = new AuthnController.PhoneRegisterRequest();
        req.setPhone(PHONE);
        req.setCode("123456");
        req.setPassword("secret123");
        when(userService.registerByPhone(anyString(), any(), any(), anyString())).thenReturn(200L);
        when(userService.findById(200L)).thenReturn(Optional.of(user(200L, null)));

        Result<RegisterResult> r = controller.registerPhone(req);

        assertTrue(r.isSuccess());
        assertNull(r.getData().getToken(), "自助注册不得签发 SSO 闸门认可的 token");
        verify(jwtUtil, never()).generateToken(anyMap(), anyLong());
    }

    // ------------------------------------------------------------------
    // 第 3 项: 免登录端点的频率限制（send-code / reset-password）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("send-code 按客户端 IP 限流: 超额 ⇒ 429 且不再真的发码")
    void sendCodeIsRateLimitedPerClientIp() {
        ReflectionTestUtils.setField(controller, "maxSendCodePerMinutePerIp", 2);
        // scene=REGISTER 的预检: 该邮箱未注册 ⇒ 允许发码
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(verifyCodeService.sendCode(anyString(), anyString(), anyString()))
                .thenReturn(sendOutcome(true, 60L, null));
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("203.0.113.9");

        AuthnController.SendCodeRequest req = new AuthnController.SendCodeRequest();
        req.setReceiver(EMAIL);
        req.setChannel(VerifyCodeService.CHANNEL_EMAIL);
        req.setScene(VerifyCodeService.SCENE_REGISTER);

        assertTrue(controller.sendCode(req, httpRequest).isSuccess());
        assertTrue(controller.sendCode(req, httpRequest).isSuccess());
        Result<Map<String, Object>> third = controller.sendCode(req, httpRequest);

        assertEquals(429, third.getCode());
        verify(verifyCodeService, times(2)).sendCode(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("send-code 按接收方限流: 超额 ⇒ 429（跨 scene 计数，防换 scene 绕过冷却）")
    void sendCodeIsRateLimitedPerReceiver() {
        ReflectionTestUtils.setField(controller, "maxSendCodePerHourPerReceiver", 2);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(verifyCodeService.sendCode(anyString(), anyString(), anyString()))
                .thenReturn(sendOutcome(true, 60L, null));
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("203.0.113.10");

        AuthnController.SendCodeRequest req = new AuthnController.SendCodeRequest();
        req.setReceiver(EMAIL);
        req.setChannel(VerifyCodeService.CHANNEL_EMAIL);
        req.setScene(VerifyCodeService.SCENE_REGISTER);

        assertTrue(controller.sendCode(req, httpRequest).isSuccess());
        assertTrue(controller.sendCode(req, httpRequest).isSuccess());
        Result<Map<String, Object>> third = controller.sendCode(req, httpRequest);

        assertEquals(429, third.getCode());
        verify(verifyCodeService, times(2)).sendCode(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("reset-password 按接收方限流: 超额 ⇒ 429")
    void resetPasswordIsRateLimitedPerReceiver() {
        ReflectionTestUtils.setField(controller, "maxResetPasswordPerHourPerReceiver", 2);
        // 没有发过验证码 ⇒ verifyCode 抛错；限流计数发生在验证码校验之前 ⇒ 第 3 次直接 429
        doThrow(new IllegalArgumentException("请先获取验证码"))
                .when(verifyCodeService)
                .verifyCode(anyString(), anyString(), anyString(), anyString());

        AuthnController.ResetPasswordByCodeRequest req = new AuthnController.ResetPasswordByCodeRequest();
        req.setReceiver(EMAIL);
        req.setChannel(VerifyCodeService.CHANNEL_EMAIL);
        req.setCode("123456");
        req.setNewPassword("newSecret123");

        assertEquals(400, controller.resetPassword(req).getCode());
        assertEquals(400, controller.resetPassword(req).getCode());
        assertEquals(429, controller.resetPassword(req).getCode());
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static UserDO user(Long id, String email) {
        UserDO u = new UserDO();
        u.setId(id);
        u.setUserNo("U" + id);
        u.setUsername("u" + id);
        u.setNickname("u" + id);
        u.setEmail(email);
        u.setPhone(PHONE);
        u.setStatus(1);
        return u;
    }

    /**
     * {@code VerifyCodeService.SendOutcome} 的构造器是包可见的 ⇒ 这里用反射造一个「发送成功」的结果。
     */
    private static VerifyCodeService.SendOutcome sendOutcome(boolean sent, long cooldown, String message) {
        try {
            java.lang.reflect.Constructor<VerifyCodeService.SendOutcome> ctor =
                    VerifyCodeService.SendOutcome.class
                            .getDeclaredConstructor(boolean.class, long.class, String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(sent, cooldown, message);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("构造 SendOutcome 失败", e);
        }
    }
}
