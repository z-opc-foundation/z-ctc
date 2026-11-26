package com.zifang.ctc.web.api;

import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.AuthDO;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.mapper.AccountMapper;
import com.zifang.ctc.core.domain.mapper.AuthMapper;
import com.zifang.ctc.core.domain.mapper.LoginLogMapper;
import com.zifang.ctc.core.domain.mapper.MembershipMapper;
import com.zifang.ctc.core.domain.mapper.UserMapper;
import com.zifang.ctc.core.service.AccountService;
import com.zifang.ctc.core.service.AccountWithAuth;
import com.zifang.ctc.core.service.AuthService;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.core.service.UserService;
import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.config.SsoProperties;
import com.zifang.ctc.core.vo.LoginResponseVO;
import com.zifang.ctc.web.api.request.LoginRequest;
import com.zifang.ctc.web.api.response.RegisterResult;
import com.zifang.ctc.web.service.VerifyCodeService;
import com.zifang.util.core.meta.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * **身份串号**守卫（卡 TASK-20261023-037）。
 *
 * <p>病根：{@code z_ctc_ac_account.id} 与 {@code z_ctc_ac_user.id} 是<b>两套独立 id 空间</b>，
 * 而 {@code createAccountFallback} 把 {@code account.id = user.id} 且撞号时<b>静默跳过</b>
 * ⇒ 注册「成功」但没建 account 行 ⇒ 登录按 {@code auth.user_id} 反查到的
 * <b>是别人的 account 行</b> ⇒ token 的 staffId / username / accountNo / tenantCode 全是别人的
 * （实测：userId=1004 的 token 里 {@code staffId=ops}）。</p>
 *
 * <p>本测试钉住三件事：</p>
 * <ol>
 *   <li><b>撞号 ⇒ 注册明确失败</b>（不许静默跳过、不许产出「用户 A + 身份 B」）；</li>
 *   <li><b>登录侧归属校验</b>：凭证解析出的 account 行不属于该凭证的 user ⇒ 拒签 token；</li>
 *   <li><b>改密不碰他人账号</b>：撞号时不得去改别人 account 行的密码。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class AuthnControllerAccountIdentityTest {

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
     * {@code @InjectMocks} 只会走构造器 ⇒ 这里显式逐个注入。
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

    private static final String EMAIL = "mine@example.com";
    private static final String PASSWORD = "secret123";
    private static final long USER_ID = 1004L;
    private static final String USER_NO = "U1700000000000";

    // ------------------------------------------------------------------
    // 第 1 组: 注册路径
    // ------------------------------------------------------------------

    @Test
    @DisplayName("连续撞号 ⇒ 注册明确失败 409, 不建号, 每次尝试都回滚")
    void registerFailsLoudlyWhenAccountIdAlwaysCollides() {
        UserDO me = user(USER_ID);

        // 每次尝试都撞上（同一个 user.id 被他人账号占着）
        when(userService.register(anyString(), any(), any(), anyString())).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(Optional.of(me));
        when(accountMapper.selectById(USER_ID)).thenReturn(foreignAccount(USER_ID));
        when(authMapper.selectByIdentity(1, EMAIL)).thenReturn(credential(77L, USER_ID));
        when(userMapper.selectById(USER_ID)).thenReturn(me);

        Result<RegisterResult> r = controller.register(registerRequest());

        assertEquals(409, r.getCode(), "连续撞号必须明确失败, 不许返回 200");
        assertNotNull(r.getMessage());
        assertTrue(r.getMessage().contains("冲突"), "错误信息要说明是账号编号冲突: " + r.getMessage());
        // 不许把别人的 account 行当成自己的（更不许改它）
        verify(accountService, never()).createAccount(any(), anyString());
        // 每次尝试都回滚 ⇒ 邮箱不被占用, 用户可重试
        verify(authMapper, times(5)).deleteById(77L);
        verify(userMapper, times(5)).deleteById(USER_ID);
        // 不许签出任何登录态
        verify(jwtUtil, never()).generateToken(anyMap(), anyLong());
    }

    @Test
    @DisplayName("撞号 ⇒ 回滚本次注册并换 id 重试, 换到没被占的 id 后注册成功")
    void registerRetriesWithAnotherIdAfterCollision() {
        UserDO first = user(1004L);
        UserDO second = user(1005L);

        // 第 1 次拿到 1004（撞上 ops），回滚后重试拿到 1005（没被占）
        when(userService.register(anyString(), any(), any(), anyString()))
                .thenReturn(1004L).thenReturn(1005L);
        when(userService.findById(1004L)).thenReturn(Optional.of(first));
        when(userService.findById(1005L)).thenReturn(Optional.of(second));
        when(accountMapper.selectById(1004L)).thenReturn(foreignAccount(1004L));
        when(accountMapper.selectById(1005L)).thenReturn(null);
        when(authMapper.selectByIdentity(1, EMAIL)).thenReturn(credential(77L, 1004L));
        when(userMapper.selectById(1004L)).thenReturn(first);

        Result<RegisterResult> r = controller.register(registerRequest());

        assertTrue(r.isSuccess(), "换 id 后应当注册成功: " + r.getMessage());
        assertNotNull(r.getData());
        assertEquals(1005L, r.getData().getUserId());
        // 撞号那一次必须回滚, 成功的这一次必须建出**自己**的 account 行
        verify(authMapper).deleteById(77L);
        verify(userMapper).deleteById(1004L);
        ArgumentCaptor<AccountDO> cap = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountService).createAccount(cap.capture(), eq(PASSWORD));
        assertEquals(1005L, cap.getValue().getId());
        assertEquals(second.getUsername(), cap.getValue().getUsername());
    }

    @Test
    @DisplayName("撞号且他人 account 的用户名恰好相同 ⇒ 仍判为撞号（account_no 不等就不放行）")
    void registerStillFailsWhenForeignAccountHasSameUsername() {
        UserDO me = user(USER_ID);
        AccountDO foreign = foreignAccount(USER_ID);
        foreign.setUsername(me.getUsername());   // 用户名撞上, 但 account_no 不是本人的

        when(userService.register(anyString(), any(), any(), anyString())).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(Optional.of(me));
        when(accountMapper.selectById(USER_ID)).thenReturn(foreign);
        when(authMapper.selectByIdentity(1, EMAIL)).thenReturn(credential(77L, USER_ID));
        when(userMapper.selectById(USER_ID)).thenReturn(me);

        Result<RegisterResult> r = controller.register(registerRequest());

        assertEquals(409, r.getCode());
        verify(accountService, never()).createAccount(any(), anyString());
    }

    @Test
    @DisplayName("不撞号 ⇒ 注册成功, 且建出的 account 行是自己那条（username/email 与注册值一致）")
    void registerCreatesOwnAccountRow() {
        UserDO me = user(USER_ID);

        when(userService.register(anyString(), any(), any(), anyString())).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(Optional.of(me));
        when(accountMapper.selectById(USER_ID)).thenReturn(null);

        Result<RegisterResult> r = controller.register(registerRequest());

        assertTrue(r.isSuccess());
        ArgumentCaptor<AccountDO> cap = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountService).createAccount(cap.capture(), eq(PASSWORD));
        AccountDO created = cap.getValue();
        assertEquals(USER_ID, created.getId());
        assertEquals(me.getUsername(), created.getUsername());
        assertEquals(EMAIL, created.getEmail());
        assertEquals(USER_NO, created.getAccountNo());
    }

    @Test
    @DisplayName("account 行已属于本人 ⇒ 幂等, 不重复建号, 注册照常成功")
    void registerIsIdempotentWhenAccountRowAlreadyOwned() {
        UserDO me = user(USER_ID);

        when(userService.register(anyString(), any(), any(), anyString())).thenReturn(USER_ID);
        when(userService.findById(USER_ID)).thenReturn(Optional.of(me));
        when(accountMapper.selectById(USER_ID)).thenReturn(ownAccount(USER_ID, me));

        Result<RegisterResult> r = controller.register(registerRequest());

        assertTrue(r.isSuccess());
        verify(accountService, never()).createAccount(any(), anyString());
    }

    // ------------------------------------------------------------------
    // 第 2 组: 登录路径（token claim 必须全部来自被注册的用户）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("登录撞号（account 行是别人的）⇒ 拒签 token, 不产出「用户 A + 身份 B」")
    void loginRejectsForeignAccountRow() {
        UserDO me = user(USER_ID);
        AccountDO foreign = foreignAccount(USER_ID);
        AuthDO cred = credential(77L, USER_ID);

        when(authService.findAccountByCredential(1, EMAIL, null))
                .thenReturn(Optional.of(new AccountWithAuth(foreign, cred)));
        when(authService.verifyPassword(PASSWORD, cred.getCredential())).thenReturn(true);
        when(userMapper.selectById(USER_ID)).thenReturn(me);

        Result<LoginResponseVO> r = controller.login(loginRequest(), mock(HttpServletRequest.class),
                mock(HttpServletResponse.class), "unit-test");

        assertEquals(409, r.getCode(), "撞号时必须拒签, 不许签出别人的身份");
        verify(jwtUtil, never()).generateToken(anyMap(), anyLong());
    }

    @Test
    @DisplayName("登录正常 ⇒ token 的 userId/staffId/username/tenantCode 全部来自本人")
    void loginTokenClaimsComeFromTheRegisteredUser() {
        UserDO me = user(USER_ID);
        AccountDO own = ownAccount(USER_ID, me);
        AuthDO cred = credential(77L, USER_ID);

        when(authService.findAccountByCredential(1, EMAIL, null))
                .thenReturn(Optional.of(new AccountWithAuth(own, cred)));
        when(authService.verifyPassword(PASSWORD, cred.getCredential())).thenReturn(true);
        when(userMapper.selectById(USER_ID)).thenReturn(me);
        when(jwtUtil.generateToken(anyMap(), anyLong())).thenReturn("jwt-token");

        Result<LoginResponseVO> r = controller.login(loginRequest(), mock(HttpServletRequest.class),
                mock(HttpServletResponse.class), "unit-test");

        assertTrue(r.isSuccess());
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        verify(jwtUtil).generateToken(cap.capture(), anyLong());
        Map<String, Object> claims = cap.getValue();
        assertEquals(String.valueOf(USER_ID), claims.get("userId"));
        assertEquals(own.getAccountNo(), claims.get("staffId"));
        assertEquals(own.getUsername(), claims.get("username"));
        assertEquals(own.getTenantCode(), claims.get("tenantCode"));
        assertFalse(String.valueOf(claims.get("staffId")).contains("ops"));
    }

    // ------------------------------------------------------------------
    // 第 3 组: 改密不得碰他人账号
    // ------------------------------------------------------------------

    @Test
    @DisplayName("改密撞号 ⇒ 不去改他人 account 行的密码")
    void resetPasswordDoesNotTouchForeignAccountRow() {
        UserDO me = user(USER_ID);

        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(me));
        when(accountMapper.selectById(USER_ID)).thenReturn(foreignAccount(USER_ID));

        AuthnController.ResetPasswordByCodeRequest req = new AuthnController.ResetPasswordByCodeRequest();
        req.setReceiver(EMAIL);
        req.setChannel(VerifyCodeService.CHANNEL_EMAIL);
        req.setCode("123456");
        req.setNewPassword("newSecret123");

        Result<Void> r = controller.resetPassword(req);

        assertTrue(r.isSuccess());
        verify(accountService, never()).resetPassword(anyLong(), anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static AuthnController.RegisterRequest registerRequest() {
        AuthnController.RegisterRequest req = new AuthnController.RegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword(PASSWORD);
        req.setCode("123456");
        req.setUsername("mine");
        req.setNickname("mine");
        return req;
    }

    private static LoginRequest loginRequest() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier(EMAIL);
        req.setPassword(PASSWORD);
        req.setIdentityType(1);
        return req;
    }

    private static UserDO user(Long id) {
        UserDO u = new UserDO();
        u.setId(id);
        u.setUserNo(USER_NO);
        u.setUsername("mine");
        u.setNickname("mine");
        u.setEmail(EMAIL);
        u.setPhone(null);
        u.setStatus(1);
        return u;
    }

    /**
     * 本人的 account 行（{@code createAccountFallback} 写出来的那条）.
     */
    private static AccountDO ownAccount(Long id, UserDO owner) {
        AccountDO a = new AccountDO();
        a.setId(id);
        a.setAccountNo(owner.getUserNo());
        a.setUsername(owner.getUsername());
        a.setNickname(owner.getNickname());
        a.setEmail(owner.getEmail());
        a.setPhone(owner.getPhone());
        a.setAccountType(2);
        a.setTenantCode("default");
        a.setStatus(1);
        return a;
    }

    /**
     * **他人**的 account 行（撞号现场：email/phone 都是 NULL, 只有 account_no 是 {@code ops}）.
     */
    private static AccountDO foreignAccount(Long id) {
        AccountDO a = new AccountDO();
        a.setId(id);
        a.setAccountNo("ops");
        a.setUsername("ops");
        a.setNickname("ops");
        a.setEmail(null);
        a.setPhone(null);
        a.setAccountType(2);
        a.setTenantCode("default");
        a.setStatus(1);
        return a;
    }

    private static AuthDO credential(Long id, Long userId) {
        AuthDO auth = new AuthDO();
        auth.setId(id);
        auth.setUserId(userId);
        auth.setIdentityType(1);
        auth.setIdentifier(EMAIL);
        auth.setCredential("bcrypt-hash");
        auth.setStatus(1);
        return auth;
    }
}
