package com.zifang.ctc.web.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zifang.ctc.common.vo.VerifyResponseVO;
import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.LoginLogDO;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.mapper.AccountMapper;
import com.zifang.ctc.core.domain.mapper.LoginLogMapper;
import com.zifang.ctc.core.domain.mapper.MembershipMapper;
import com.zifang.ctc.core.domain.mapper.UserMapper;
import com.zifang.ctc.core.service.AccountWithAuth;
import com.zifang.ctc.core.service.AuthService;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.core.service.UserService;
import com.zifang.ctc.core.vo.AccountVO;
import com.zifang.ctc.core.vo.LoginResponseVO;
import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.config.SsoContext;
import com.zifang.ctc.sso.config.SsoProperties;
import com.zifang.ctc.sso.model.UserInfo;
import com.zifang.ctc.web.api.request.LoginRequest;
import com.zifang.ctc.web.api.request.SwitchTenantRequest;
import com.zifang.ctc.web.api.response.MyTenantsResult;
import com.zifang.ctc.web.api.response.RegisterResult;
import com.zifang.ctc.web.service.VerifyCodeService;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.zifang.util.core.meta.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 认证 Controller — 登录 / 验证.
 * <p>
 * 与 z-ctc-sso 集成: 登录成功用 JwtUtil 发 token, 后续请求由 z-ctc-sso 的 SsoInterceptor 拦截验证.
 * <p>
 * 设计哲学:
 * 4A 中心的 "A1 Authentication" 域 = "证明你是你" 这一步 — 不接管授权, 不接管账号生命周期,
 * 业务方按需在 token 中塞入 userId/username/nickname/tenantCode/role 等 claim.
 */
@Tag(name = "4A-认证 (authn)")
@RestController
@RequestMapping("/api/ctc/authn")
public class AuthnController {

    private static final Logger log = LogManager.getLogger(AuthnController.class);
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_ATTEMPT_WINDOW_MILLIS = 60_000L;

    private final AuthService authService;
    private final LoginLogMapper loginLogMapper;
    private final Cache<String, LoginAttempt> loginAttempts = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100_000L)
            .build();

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private SsoProperties ssoProperties;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MembershipMapper membershipMapper;
    @Autowired
    private com.zifang.ctc.core.service.AccountService accountService;
    @Autowired
    private UserService userService;
    @Autowired
    private MembershipService membershipService;
    @Autowired
    private com.zifang.ctc.core.domain.mapper.AuthMapper authMapper;
    @Autowired
    private VerifyCodeService verifyCodeService;

    public AuthnController(AuthService authService, LoginLogMapper loginLogMapper) {
        this.authService = authService;
        this.loginLogMapper = loginLogMapper;
    }

    /**
     * 构造登录限流键, 仅按用户标识计数, 防止通过租户、凭证类型、大小写或首尾空格绕过限制.
     */
    private static String loginAttemptKey(String identifier) {
        return identifier.trim().toLowerCase(Locale.ROOT);
    }

    // ========================================================================
    // FEATURE049: 多租户身份模型 — 新增端点
    // ========================================================================

    /**
     * 账号密码登录. 成功返回 token + 账号信息; 失败返回 401.
     */
    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public Result<LoginResponseVO> login(@RequestBody LoginRequest request,
                                         HttpServletResponse response,
                                         @RequestHeader(value = "X-Client-Ip", required = false) String clientIp,
                                         @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        if (request == null || request.getIdentifier() == null || request.getPassword() == null) {
            return Result.<LoginResponseVO>fail("identifier / password 必填").code(400);
        }
        int identityType = request.getIdentityType() == null ? 1 : request.getIdentityType();
        String attemptKey = loginAttemptKey(request.getIdentifier());
        if (!acquireLoginAttempt(attemptKey)) {
            return Result.<LoginResponseVO>fail("登录尝试过于频繁，请1分钟后再试").code(429);
        }

        Optional<AccountWithAuth> result = authService.findAccountByCredential(
                identityType, request.getIdentifier(), request.getTenantCode());

        String loginFailureReason = null;
        if (!result.isPresent()) {
            loginFailureReason = "账号不存在或已禁用";
        } else if (identityType == 1) {
            // 密码凭证才校验密码
            AccountWithAuth aw = result.get();
            if (aw.auth.getCredential() == null
                    || !authService.verifyPassword(request.getPassword(), aw.auth.getCredential())) {
                loginFailureReason = "密码错误";
            }
        }
        if (loginFailureReason != null) {
            recordLoginLog(result.map(accountWithAuth -> accountWithAuth.account).orElse(null),
                    request.getIdentifier(), identityType, 0, loginFailureReason, clientIp, userAgent, request.getTenantCode());
            return Result.<LoginResponseVO>fail(loginFailureReason).code(401);
        }

        AccountWithAuth aw = result.get();
        recordLoginLog(aw.account, request.getIdentifier(), identityType, 1, null, clientIp, userAgent, request.getTenantCode());

        // 签发 token: tenantCode 优先用 request.tenantCode (从 URL/前端传入)
        // domainCode 不在账号主表, 只能从 request 拿 (URL 决定租户/域)
        String token = issueToken(aw.account, request.getTenantCode(), request.getDomainCode());
        LoginResponseVO body = new LoginResponseVO(token, AccountVO.from(aw.account));

        // 写 SSO cookie (跨子域共享, e.g. .zopc.top)
        writeSsoCookie(response, token);

        loginAttempts.invalidate(attemptKey);
        log.info("AuthnController.login 成功: accountId={}, username={}, tenant={}, domain={}, source={}",
                aw.account.getId(), aw.account.getUsername(),
                request.getTenantCode(), request.getDomainCode(), request.getSource());
        return Result.success(body);
    }

    /**
     * 写 SSO cookie. 默认 Domain=.zopc.top 让 opc/ppt 共享登录态.
     * Secure flag: HTTPS 站点为 true; HTTP 测试环境为 false.
     */
    private void writeSsoCookie(HttpServletResponse response, String token) {
        String cookieName = ssoProperties.getTokenCookieName();
        String domain = ssoProperties.getCookieDomain();
        boolean secure = ssoProperties.isCookieSecure();

        StringBuilder sb = new StringBuilder();
        sb.append(cookieName).append("=").append(token).append("; Path=/; HttpOnly; SameSite=Lax");
        if (domain != null && !domain.isEmpty()) {
            sb.append("; Domain=").append(domain);
        }
        if (secure) {
            sb.append("; Secure");
        }
        // 8 小时 (与 JWT 一致)
        sb.append("; Max-Age=").append(8 * 60 * 60);
        response.addHeader("Set-Cookie", sb.toString());
        // 不再直接 System.out 输出 SSO cookie 明文, 避免标准输出泄露 token.
        if (log.isDebugEnabled()) {
            // 调试场景仅打印 cookie 名 + 长度, 不打印 token 本身.
            log.debug("writeSsoCookie: name={}, valueLen={}, domain={}, secure={}",
                    cookieName, token == null ? 0 : token.length(), domain, secure);
        }
    }

    /**
     * 验证 token 是否有效 (兼容 z-ctc-sso 的 RemoteTokenService 调用).
     */
    @Operation(summary = "验证token有效性")
    @GetMapping("/verify")
    public Result<VerifyResponseVO> verify(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.<VerifyResponseVO>fail("missing Authorization Bearer").code(401);
        }
        String token = authHeader.substring(7);
        JwtUtil.VerificationResult v = jwtUtil.verifyToken(token);
        if (!v.isValid()) {
            return Result.<VerifyResponseVO>fail("token 无效: " + v.getErrorMessage()).code(401);
        }
        return Result.success(new VerifyResponseVO(v.getClaims(), true));
    }

    /**
     * 获取当前登录用户信息.
     * <p>
     * 适用场景: PPT 等第三方应用通过 cookie 携带 token, 主控 /api/ctc/authn/whoami 拿用户上下文.
     * <p>
     * FEATURE-ZTEAM-AUTH-UNIFY: 原本走 SsoContext.getCurrentUser() 依赖 SsoInterceptor,
     * 但 {@code /api/ctc/authn/**} 在 sso.exclude-paths 中 (不能删, login / switch-tenant 等需要免密),
     * 导致 whoami 永远拿不到 user. 改为本方法自行从 Authorization/Cookie 解析 token,
     * 与 SsoInterceptor 解耦.
     */
    @Operation(summary = "当前登录用户")
    @GetMapping("/whoami")
    public Result<UserInfo> whoami(HttpServletRequest request) {
        try {
            UserInfo user = extractUserFromRequest(request);
            if (user == null) {
                return Result.<UserInfo>fail("未登录").code(401);
            }
            return Result.success(user);
        } catch (Exception e) {
            log.warn("[Authn] whoami 异常, 降级返回未登录: {}", e.toString());
            return Result.<UserInfo>fail("未登录").code(401);
        }
    }

    /**
     * 退出登录 — 清浏览器 sso_token Cookie (按 sso.token-cookie-names 顺序逐个清),
     * 与 z-team 历史 TeamAuthnController.logout 行为对齐。
     * <p>
     * FEATURE-ZTEAM-AUTH-UNIFY: ctc 此前没有 logout, 业务方都自己清 cookie。
     * 现在 4A 中心统一处理, 各产品不再需要自己的 /api/authn/logout。
     * 鉴权: 因为 /api/ctc/authn/** 在 sso.exclude-paths, 任意调用方都能清自己的 cookie,
     * 这是 by design — 退出登录本来就该公开。
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            java.util.List<String> cookieNames = ssoProperties.getEffectiveTokenCookieNames();
            String domain = ssoProperties.getCookieDomain();
            boolean secure = ssoProperties.isCookieSecure();
            int cleared = 0;
            for (String name : cookieNames) {
                if (name == null || name.trim().isEmpty()) { continue; }

                StringBuilder sb = new StringBuilder();
                sb.append(name).append("=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
                if (domain != null && !domain.trim().isEmpty()) {
                    sb.append("; Domain=").append(domain.trim());
                }
                if (secure) {
                    sb.append("; Secure");
                }
                response.addHeader("Set-Cookie", sb.toString());
                cleared++;
            }
            log.info("[Authn] logout 已下发 {} 条 Set-Cookie Max-Age=0 (cookieNames={})", cleared, cookieNames);
            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("ok", true);
            resp.put("clearedCookieCount", cleared);
            return Result.success(resp);
        } catch (Exception e) {
            log.warn("[Authn] logout 异常, 仍返回 ok: {}", e.toString());
            return Result.<Map<String, Object>>success(java.util.Collections.singletonMap("ok", true));
        }
    }

    /**
     * 从 Authorization / Cookie / Query 参数中解析 token, 验证后构造 UserInfo.
     * <p>顺序: Authorization Bearer > Cookie (按 sso.token-cookie-names 顺序) > ?token= 参数.
     */
    private UserInfo extractUserFromRequest(HttpServletRequest request) {
        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (authHeader != null) {
            token = authHeader;
        }
        if (token == null) {
            javax.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null && ssoProperties != null) {
                java.util.List<String> candidateNames = ssoProperties.getEffectiveTokenCookieNames();
                for (String name : candidateNames) {
                    for (javax.servlet.http.Cookie c : cookies) {
                        if (name.equals(c.getName())) {
                            String v = c.getValue();
                            if (v != null && !v.isEmpty()) {
                                token = v;
                                break;
                            }
                        }
                    }
                    if (token != null) { break; }
                }
            }
        }
        if (token == null) {
            token = request.getParameter("token");
        }
        if (token == null || token.isEmpty()) {
            return null;
        }
        JwtUtil.VerificationResult v = jwtUtil.verifyToken(token);
        if (!v.isValid()) {
            return null;
        }
        java.util.Map<String, Object> claims = v.getClaims();
        UserInfo u = new UserInfo();
        u.setUserId(String.valueOf(claims.getOrDefault("userId", "")));
        u.setUsername(String.valueOf(claims.getOrDefault("username", "")));
        u.setNickname(String.valueOf(claims.getOrDefault("nickname", "")));
        Object tenant = claims.get("tenantCode");
        if (tenant != null) { u.setTenantCode(String.valueOf(tenant)); }

        Object domain = claims.get("domainCode");
        if (domain != null) { u.setDomainCode(String.valueOf(domain)); }

        return u;
    }

    /**
     * 切换当前登录用户的租户 / 域 — 重新签发 JWT, 让后续请求带上新的 tenantCode/domainCode claim.
     * <p>
     * FEATURE012 增量: 之前切换租户只是改了前端 localStorage.z_tenant, 后端 JWT claim 没变.
     * 现在前端右上角切租户/域时调这个接口, 后端:
     * 1. 从 Authorization 头解析当前 JWT, 拿到 userId
     * 2. 校验目标 tenantCode (允许为空字符串 = 清空当前租户)
     * 3. 重新签发 8h JWT, 覆盖 tenantCode + 写入 domainCode claim
     * 4. 返回新 token + 更新后的 account 视图 (含 tenantCode/domainCode)
     * <p>
     * 注: 因为 /api/ctc/authn/** 在 application.properties 里是 sso.exclude-paths,
     * 所以本接口不会经过 SsoInterceptor, 也不依赖 SsoContext — 身份从 header 里直接取.
     */
    @Operation(summary = "切换租户/域")
    @PostMapping("/switch-tenant")
    public Result<LoginResponseVO> switchTenant(@RequestBody SwitchTenantRequest request,
                                                HttpServletRequest httpRequest) {
        if (request == null) {
            return Result.<LoginResponseVO>fail("请求体为空").code(400);
        }

        // 1. 从 Authorization 头拿旧 token, 校验通过后拿到 userId
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.<LoginResponseVO>fail("缺少 Bearer token").code(401);
        }
        String oldToken = authHeader.substring(7);
        JwtUtil.VerificationResult v = jwtUtil.verifyToken(oldToken);
        if (!v.isValid()) {
            return Result.<LoginResponseVO>fail("当前 token 无效: " + v.getErrorMessage()).code(401);
        }
        Object userIdObj = v.getClaims().get("userId");
        if (userIdObj == null || String.valueOf(userIdObj).isEmpty()) {
            return Result.<LoginResponseVO>fail("当前 token 缺少 userId claim").code(401);
        }
        Long accountId;
        try {
            accountId = Long.parseLong(String.valueOf(userIdObj));
        } catch (NumberFormatException nfe) {
            return Result.<LoginResponseVO>fail("userId claim 非数字").code(401);
        }

        // 2. 拉账号最新信息 (用户名/昵称/角色 都不丢)
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null) {
            return Result.<LoginResponseVO>fail("账号不存在或已被删除").code(401);
        }
        if (account.getStatus() != null && account.getStatus() != 1) {
            return Result.<LoginResponseVO>fail("账号已禁用").code(403);
        }

        // 3. 应用新的租户 / 域
        String newTenant = request.getTenantCode();
        String newDomain = request.getDomainCode();
        // domainCode 允许为空 (前端切租户时还没选域); tenantCode 允许为空字符串 (清空)
        // 但不允 null
        if (newTenant == null) { newTenant = ""; }

        if (newDomain == null) { newDomain = ""; }


        // 4. 重新签发 JWT
        String newToken = issueToken(account, newTenant, newDomain);

        AccountVO accountVO = AccountVO.from(account);
        accountVO.setTenantCode(newTenant);
        // 注: AccountVO 没有 domainCode 字段, 放在 extra 里或通过新字段扩展
        LoginResponseVO body = new LoginResponseVO(newToken, accountVO);
        log.info("AuthnController.switchTenant 成功: accountId={}, newTenant={}, newDomain={}",
                account.getId(), newTenant, newDomain);
        return Result.success(body);
    }

    /**
     * FEATURE049: 列出当前用户的所有 active 租户.
     * 用于登录后前端展示"租户选择器"或"自动进入单租户".
     * <p>
     * 返回结构: { users: [...], memberships: [{userId, tenantCode, roleCode, ...}], tenantCount: N }
     */
    @Operation(summary = "我的租户列表 (FEATURE049)")
    @GetMapping("/my-tenants")
    public Result<MyTenantsResult> myTenants(HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        if (userId == null) { return Result.<MyTenantsResult>fail("未登录或 token 缺少 userId").code(401); }


        UserDO user = userMapper.selectById(userId);
        if (user == null) { return Result.<MyTenantsResult>fail("用户不存在").code(404); }


        java.util.List<MembershipDO> memberships = membershipMapper.selectActiveByUserId(userId);
        MyTenantsResult body = new MyTenantsResult();
        body.setUserId(userId);
        body.setUserNo(user.getUserNo());
        body.setEmail(user.getEmail());
        body.setUsername(user.getUsername());
        body.setNickname(user.getNickname());
        body.setMemberships(memberships);
        body.setTenantCount(memberships.size());
        body.setOrphan(memberships.isEmpty());
        return Result.success(body);
    }

    /**
     * FEATURE049: 选择租户 (阶段二) — 校验 membership 存在, 签发带 tenantCode 的正式 token.
     * <p>
     * 注: 阶段一登录后, 前端拿到的是临时 token (无 tenantCode). 前端调本接口锁定租户,
     * 后端校验该 user 在该 tenant 下有 active membership, 然后签发带 tenantCode 的正式 token.
     */
    @Operation(summary = "选择租户 (阶段二, FEATURE049)")
    @PostMapping("/select-tenant")
    public Result<LoginResponseVO> selectTenant(@RequestBody SwitchTenantRequest request,
                                                HttpServletRequest httpRequest) {
        if (request == null || request.getTenantCode() == null) {
            return Result.<LoginResponseVO>fail("tenantCode 必填").code(400);
        }
        Long userId = extractUserId(httpRequest);
        if (userId == null) { return Result.<LoginResponseVO>fail("未登录或 token 缺少 userId").code(401); }


        UserDO user = userMapper.selectById(userId);
        if (user == null) { return Result.<LoginResponseVO>fail("用户不存在").code(404); }

        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.<LoginResponseVO>fail("用户已禁用").code(403);
        }

        // 校验 membership
        MembershipDO ms = membershipMapper.selectByUserAndTenant(userId, request.getTenantCode());
        if (ms == null) {
            return Result.<LoginResponseVO>fail("您不是该租户成员, 请先加入").code(403);
        }
        if (ms.getMemberStatus() == null || ms.getMemberStatus() != 1) {
            return Result.<LoginResponseVO>fail("成员关系已禁用或已退出").code(403);
        }

        // 兼容旧 account 表 (FEATURE049 过渡期)
        AccountDO account = accountMapper.selectById(userId);
        if (account == null) {
            return Result.<LoginResponseVO>fail("账号不存在或已被删除").code(401);
        }

        // 签发带 tenantCode 的正式 token
        String newToken = issueToken(account, request.getTenantCode(), request.getDomainCode());
        AccountVO accountVO = AccountVO.from(account);
        accountVO.setTenantCode(request.getTenantCode());
        LoginResponseVO body = new LoginResponseVO(newToken, accountVO);
        log.info("AuthnController.selectTenant 成功: userId={}, tenant={}, role={}",
                userId, request.getTenantCode(), ms.getRoleCode());
        return Result.success(body);
    }

    /**
     * FEATURE049: 注册接口 (创建游离 user, 不建 membership).
     * 阶段: 用户注册后跳转 "选择组织" 页 (创建租户 / 输入邀请码).
     * <p>
     * 重构: 改用 UserService.register, 业务层统一管 user + auth 双写, 避免反射调 token.
     */
    @Operation(summary = "注册 (FEATURE049, 创建游离 user)")
    @PostMapping("/register")
    public Result<RegisterResult> register(@RequestBody RegisterRequest req) {
        try {
            if (req == null || req.getEmail() == null || req.getPassword() == null) {
                return Result.<RegisterResult>fail("email / password 必填").code(400);
            }
            if (req.getPassword().length() < 6) {
                return Result.<RegisterResult>fail("密码至少 6 位").code(400);
            }
            // 委托给 UserService.register (含 BCrypt 哈希 + 查重 + 写 user + 写 auth)
            Long userId;
            try {
                userId = userService.register(req.getEmail(), req.getUsername(), req.getNickname(), req.getPassword());
            } catch (IllegalStateException e) {
                // 邮箱已被注册
                return Result.<RegisterResult>fail(e.getMessage()).code(409);
            } catch (IllegalArgumentException e) {
                return Result.<RegisterResult>fail(e.getMessage()).code(400);
            }

            // 兜底: 兼容期同步写一条 account (4A 老接口仍依赖 z_ctc_ac_account)
            // TODO: 等所有 Controller 切到 UserService 后删掉这段
            createAccountFallback(userId, req.getPassword());

            // 签发临时 token (无 tenantCode, 阶段一)
            UserDO user = userService.findById(userId).orElseThrow(() -> new IllegalStateException("注册后 user 丢失"));
            AccountVO accountVO = new AccountVO();
            accountVO.setId(user.getId());
            accountVO.setUsername(user.getUsername());
            accountVO.setNickname(user.getNickname());
            accountVO.setEmail(user.getEmail());
            String token = issueTokenFromUser(user);

            RegisterResult body = new RegisterResult();
            body.setUserId(user.getId());
            body.setEmail(user.getEmail());
            body.setUsername(user.getUsername());
            body.setNickname(user.getNickname());
            body.setToken(token);
            body.setOrphan(true);
            log.info("AuthnController.register 成功: userId={}, email={}", userId, req.getEmail());
            return Result.success(body);
        } catch (Exception ex) {
            log.error("AuthnController.register 失败: {}", ex.getMessage(), ex);
            return Result.<RegisterResult>fail("register failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()).code(500);
        }
    }

    // ========================================================================
    // 验证码体系: 发送验证码 / 手机验证码登录 / 手机号注册 / 找回密码
    // ========================================================================

    /**
     * 发送验证码. channel: PHONE/EMAIL; scene: LOGIN/REGISTER/RESET.
     * PHONE 场景预检手机号注册态, 避免发无用短信.
     */
    @Operation(summary = "发送验证码 (注册/登录/找回密码)")
    @PostMapping("/send-code")
    public Result<Map<String, Object>> sendCode(@RequestBody SendCodeRequest req) {
        if (req == null || req.getReceiver() == null || req.getChannel() == null || req.getScene() == null) {
            return Result.<Map<String, Object>>fail("receiver / channel / scene 必填").code(400);
        }
        String receiver = req.getReceiver().trim();
        String channel = req.getChannel().trim().toUpperCase(Locale.ROOT);
        String scene = req.getScene().trim().toUpperCase(Locale.ROOT);
        if (!VerifyCodeService.CHANNEL_PHONE.equals(channel)
                && !VerifyCodeService.CHANNEL_EMAIL.equals(channel)) {
            return Result.<Map<String, Object>>fail("channel 仅支持 PHONE / EMAIL").code(400);
        }
        if (!VerifyCodeService.SCENE_LOGIN.equals(scene)
                && !VerifyCodeService.SCENE_REGISTER.equals(scene)
                && !VerifyCodeService.SCENE_RESET.equals(scene)) {
            return Result.<Map<String, Object>>fail("scene 仅支持 LOGIN / REGISTER / RESET").code(400);
        }
        if (VerifyCodeService.CHANNEL_PHONE.equals(channel) && !receiver.matches("^1[3-9]\\d{9}$")) {
            return Result.<Map<String, Object>>fail("手机号格式不正确").code(400);
        }

        // 场景预检: 注册要求未注册; 登录/重置要求已注册
        boolean registered = VerifyCodeService.CHANNEL_PHONE.equals(channel)
                ? userService.findByPhone(receiver).isPresent()
                : userService.findByEmail(receiver).isPresent();
        if (VerifyCodeService.SCENE_REGISTER.equals(scene) && registered) {
            return Result.<Map<String, Object>>fail("该" + (VerifyCodeService.CHANNEL_PHONE.equals(channel) ? "手机号" : "邮箱") + "已注册, 请直接登录").code(409);
        }
        if (!VerifyCodeService.SCENE_REGISTER.equals(scene) && !registered) {
            return Result.<Map<String, Object>>fail("该" + (VerifyCodeService.CHANNEL_PHONE.equals(channel) ? "手机号" : "邮箱") + "未注册").code(404);
        }

        VerifyCodeService.SendOutcome outcome = verifyCodeService.sendCode(receiver, channel, scene);
        if (!outcome.sent) {
            return Result.<Map<String, Object>>fail(outcome.message).code(429);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("codeSent", true);
        body.put("cooldownSeconds", outcome.cooldownSeconds);
        body.put("expireSeconds", 300);
        return Result.success(body);
    }

    /**
     * 手机验证码登录. 验证码校验通过后免密登录, 签发与密码登录同构的 token + SSO cookie.
     */
    @Operation(summary = "手机验证码登录")
    @PostMapping("/phone-login")
    public Result<LoginResponseVO> phoneLogin(@RequestBody PhoneLoginRequest request,
                                              HttpServletResponse response,
                                              @RequestHeader(value = "X-Client-Ip", required = false) String clientIp,
                                              @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        if (request == null || request.getPhone() == null || request.getCode() == null) {
            return Result.<LoginResponseVO>fail("phone / code 必填").code(400);
        }
        String phone = request.getPhone().trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.<LoginResponseVO>fail("手机号格式不正确").code(400);
        }
        // 与密码登录同构的限流 (同手机号 60s 内 5 次)
        String attemptKey = "phone:" + loginAttemptKey(phone);
        if (!acquireLoginAttempt(attemptKey)) {
            return Result.<LoginResponseVO>fail("登录尝试过于频繁，请1分钟后再试").code(429);
        }

        // 验证码校验 (一次性消费)
        try {
            verifyCodeService.verifyCode(phone, VerifyCodeService.CHANNEL_PHONE,
                    VerifyCodeService.SCENE_LOGIN, request.getCode().trim());
        } catch (IllegalArgumentException e) {
            recordLoginLog(null, phone, 2, 0, "验证码失败: " + e.getMessage(), clientIp, userAgent, request.getTenantCode());
            return Result.<LoginResponseVO>fail(e.getMessage()).code(401);
        }

        Optional<UserDO> userOpt = userService.findByPhone(phone);
        if (!userOpt.isPresent()) {
            recordLoginLog(null, phone, 2, 0, "手机号未注册", clientIp, userAgent, request.getTenantCode());
            return Result.<LoginResponseVO>fail("该手机号未注册").code(401);
        }
        UserDO user = userOpt.get();
        if (user.getStatus() == null || user.getStatus() != 1) {
            recordLoginLog(null, phone, 2, 0, "用户已禁用", clientIp, userAgent, request.getTenantCode());
            return Result.<LoginResponseVO>fail("用户已禁用").code(403);
        }

        // 兼容旧 account 表: 有则用真实 account (保留 staffId claim), 无则用 user 包装
        AccountDO account = accountMapper.selectById(user.getId());
        String token;
        AccountVO accountVO;
        if (account != null) {
            token = issueToken(account, request.getTenantCode(), request.getDomainCode());
            accountVO = AccountVO.from(account);
        } else {
            token = issueTokenFromUser(user);
            accountVO = new AccountVO();
            accountVO.setId(user.getId());
            accountVO.setUsername(user.getUsername());
            accountVO.setNickname(user.getNickname());
            accountVO.setPhone(user.getPhone());
        }

        writeSsoCookie(response, token);
        loginAttempts.invalidate(attemptKey);
        recordLoginLog(account, phone, 2, 1, null, clientIp, userAgent, request.getTenantCode());
        log.info("AuthnController.phoneLogin 成功: userId={}, phone={}, tenant={}, source={}",
                user.getId(), phone, request.getTenantCode(), request.getSource());
        return Result.success(new LoginResponseVO(token, accountVO));
    }

    /**
     * 手机号 + 验证码注册. 与邮箱注册同构: 创建游离 user + account 兼容兑底 + 临时 token.
     */
    @Operation(summary = "手机号注册 (验证码)")
    @PostMapping("/register-phone")
    public Result<RegisterResult> registerPhone(@RequestBody PhoneRegisterRequest req) {
        try {
            if (req == null || req.getPhone() == null || req.getCode() == null || req.getPassword() == null) {
                return Result.<RegisterResult>fail("phone / code / password 必填").code(400);
            }
            String phone = req.getPhone().trim();
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                return Result.<RegisterResult>fail("手机号格式不正确").code(400);
            }

            // 验证码校验 (一次性消费)
            try {
                verifyCodeService.verifyCode(phone, VerifyCodeService.CHANNEL_PHONE,
                        VerifyCodeService.SCENE_REGISTER, req.getCode().trim());
            } catch (IllegalArgumentException e) {
                return Result.<RegisterResult>fail(e.getMessage()).code(400);
            }

            Long userId;
            try {
                userId = userService.registerByPhone(phone, req.getUsername(), req.getNickname(), req.getPassword());
            } catch (IllegalStateException e) {
                return Result.<RegisterResult>fail(e.getMessage()).code(409);
            } catch (IllegalArgumentException e) {
                return Result.<RegisterResult>fail(e.getMessage()).code(400);
            }

            // 兑底: 兼容期同步写一条 account
            createAccountFallback(userId, req.getPassword());

            UserDO user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("注册后 user 丢失"));
            RegisterResult body = new RegisterResult();
            body.setUserId(user.getId());
            body.setPhone(phone);
            body.setUsername(user.getUsername());
            body.setNickname(user.getNickname());
            body.setToken(issueTokenFromUser(user));
            body.setOrphan(true);
            log.info("AuthnController.registerPhone 成功: userId={}, phone={}", userId, phone);
            return Result.success(body);
        } catch (Exception ex) {
            log.error("AuthnController.registerPhone 失败: {}", ex.getMessage(), ex);
            return Result.<RegisterResult>fail("register failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()).code(500);
        }
    }

    /**
     * 找回密码: 手机/邮箱验证码 + 新密码重置 (user + auth + account 三表同步).
     */
    @Operation(summary = "找回密码 (验证码重置)")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordByCodeRequest req) {
        if (req == null || req.getReceiver() == null || req.getChannel() == null
                || req.getCode() == null || req.getNewPassword() == null) {
            return Result.<Void>fail("receiver / channel / code / newPassword 必填").code(400);
        }
        String receiver = req.getReceiver().trim();
        String channel = req.getChannel().trim().toUpperCase(Locale.ROOT);
        if (!VerifyCodeService.CHANNEL_PHONE.equals(channel)
                && !VerifyCodeService.CHANNEL_EMAIL.equals(channel)) {
            return Result.<Void>fail("channel 仅支持 PHONE / EMAIL").code(400);
        }
        if (req.getNewPassword().length() < 6) {
            return Result.<Void>fail("密码至少 6 位").code(400);
        }

        // 验证码校验 (一次性消费)
        try {
            verifyCodeService.verifyCode(receiver, channel,
                    VerifyCodeService.SCENE_RESET, req.getCode().trim());
        } catch (IllegalArgumentException e) {
            return Result.<Void>fail(e.getMessage()).code(400);
        }

        Optional<UserDO> userOpt = VerifyCodeService.CHANNEL_PHONE.equals(channel)
                ? userService.findByPhone(receiver)
                : userService.findByEmail(receiver);
        if (!userOpt.isPresent()) {
            return Result.<Void>fail("账号不存在").code(404);
        }
        UserDO user = userOpt.get();

        try {
            userService.resetPassword(user.getId(), req.getNewPassword());
        } catch (IllegalArgumentException e) {
            return Result.<Void>fail(e.getMessage()).code(400);
        }

        // 兑底: 同步重置旧 account 表密码
        try {
            AccountDO account = accountMapper.selectById(user.getId());
            if (account != null) {
                accountService.resetPassword(account.getId(), req.getNewPassword(), "self");
            }
        } catch (Exception e) {
            log.warn("reset-password 兑底重置 account 密码失败: {}", e.getMessage());
        }

        log.info("AuthnController.resetPassword 成功: userId={}, channel={}", user.getId(), channel);
        return Result.success(null);
    }

    /**
     * 兑底: 兼容期同步写一条 account (4A 老接口仍依赖 z_ctc_ac_account).
     * 写失败不影响主流程.
     */
    private void createAccountFallback(Long userId, String rawPassword) {
        try {
            UserDO user = userService.findById(userId).orElse(null);
            if (user != null && accountMapper.selectById(userId) == null) {
                AccountDO account = new AccountDO();
                account.setId(user.getId());
                account.setAccountNo(user.getUserNo());
                account.setUsername(user.getUsername());
                account.setNickname(user.getNickname());
                account.setEmail(user.getEmail());
                account.setPhone(user.getPhone());
                account.setAccountType(2);  // 普通用户
                account.setStatus(1);
                accountService.createAccount(account, rawPassword);
            }
        } catch (Exception e) {
            log.warn("兑底写 account 失败, 不影响主流程: {}", e.getMessage());
        }
    }

    // FEATURE049: register 直接用 @Autowired authMapper

    /**
     * 从 user 直接签 token. 把 UserDO 临时包成 AccountDO 调 issueToken 私有方法.
     * 不再使用反射 (FEATURE049 收尾).
     */
    private String issueTokenFromUser(UserDO user) {
        AccountDO tmp = new AccountDO();
        tmp.setId(user.getId());
        tmp.setUsername(user.getUsername());
        tmp.setNickname(user.getNickname());
        tmp.setStatus(user.getStatus() != null ? user.getStatus() : 1);
        tmp.setAccountType(2);
        return issueToken(tmp, null, null);
    }

    /**
     * 提取 token 中的 userId. 失败返回 null.
     */
    private Long extractUserId(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) { return null; }
        String token = authHeader.substring(7);
        JwtUtil.VerificationResult v = jwtUtil.verifyToken(token);
        if (!v.isValid()) { return null; }
        Object userIdObj = v.getClaims().get("userId");
        if (userIdObj == null) { return null; }

        try {
            return Long.parseLong(String.valueOf(userIdObj));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 原子记录一次登录尝试. 首次尝试起 60 秒内仅允许 5 次, 窗口过期后重新计数.
     *
     * @param attemptKey 用户登录限流键
     * @return 本次尝试是否允许继续认证
     */
    private boolean acquireLoginAttempt(String attemptKey) {
        long now = System.currentTimeMillis();
        LoginAttempt attempt = loginAttempts.asMap().compute(attemptKey, (key, current) -> {
            if (current == null || now - current.windowStartedAt >= LOGIN_ATTEMPT_WINDOW_MILLIS) {
                return new LoginAttempt(1, now);
            }
            int nextCount = current.count >= MAX_LOGIN_ATTEMPTS
                    ? MAX_LOGIN_ATTEMPTS + 1
                    : current.count + 1;
            return new LoginAttempt(nextCount, current.windowStartedAt);
        });
        return attempt.count <= MAX_LOGIN_ATTEMPTS;
    }

    /**
     * 记录登录审计日志. 异常被吞掉, 写日志失败不影响登录主流程.
     *
     * @param account       登录涉及的账号, 可为 null (账号不存在场景); 为 null 时 accountId 字段写空
     * @param identifier    登录标识 (用户名/手机号/邮箱 等)
     * @param identityType  凭证类型, 1=密码
     * @param status        登录状态, 1=成功, 0=失败
     * @param failureReason 失败原因, 成功时为 null
     * @param clientIp      客户端 IP, 来自 X-Client-Ip 头
     * @param userAgent     客户端 UA, 来自 User-Agent 头
     * @param tenantCode    登录尝试关联的租户编码, 可为 null
     */
    private void recordLoginLog(AccountDO account, String identifier, int identityType,
                                int status, String failureReason, String clientIp, String userAgent, String tenantCode) {
        try {
            LoginLogDO logEntry = new LoginLogDO();
            logEntry.setAccountId(account == null ? null : account.getId());
            logEntry.setIdentifier(identifier);
            logEntry.setIdentityType(identityType);
            logEntry.setLoginStatus(status);
            logEntry.setFailureReason(failureReason);
            logEntry.setClientIp(clientIp);
            logEntry.setUserAgent(userAgent);
            logEntry.setTenantCode(tenantCode);
            logEntry.setLoginTime(java.time.LocalDateTime.now());
            loginLogMapper.insert(logEntry);
        } catch (Exception e) {
            // 日志写失败不影响登录业务
            AuthnController.log.warn("AuthnController.recordLoginLog 失败: {}", e.getMessage());
        }
    }

    /**
     * 为指定账号签发 8 小时 JWT, 注入 userId/username/nickname/tenantCode/domainCode/accountType claim.
     * 租户优先级: tenantCodeOverride (切换/前端传入) > account 上的默认租户 > 空.
     *
     * @param account            账号实体
     * @param tenantCodeOverride 强制写入的租户编码; 为 null/空时回退到 account.tenantCode
     * @param domainCode         域编码, 为 null/空时不写入对应 claim
     * @return 8 小时过期的 JWT 字符串
     */
    private String issueToken(AccountDO account, String tenantCodeOverride, String domainCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", account.getId() == null ? "" : String.valueOf(account.getId()));
        claims.put("username", account.getUsername() == null ? "" : account.getUsername());
        claims.put("nickname", account.getNickname() == null ? "" : account.getNickname());
        // 将 accountNo 作为 staffId 写入 JWT，供 z-team 使用
        if (account.getAccountNo() != null && !account.getAccountNo().isEmpty()) {
            claims.put("staffId", account.getAccountNo());
        }
        // 租户优先级: override (切换/前端传入) > account 上的默认租户 > 空
        String tenant = tenantCodeOverride;
        if (tenant == null || tenant.isEmpty()) {
            tenant = account.getTenantCode();
        }
        if (tenant != null && !tenant.isEmpty()) {
            claims.put("tenantCode", tenant);
        }
        // 域: 不在账号主表里, 切换时由前端传入; 登录默认空
        if (domainCode != null && !domainCode.isEmpty()) {
            claims.put("domainCode", domainCode);
        }
        if (account.getAccountType() != null) {
            claims.put("accountType", account.getAccountType());
        }
        // access token 默认 8 小时 (业务方按需覆盖)
        return jwtUtil.generateToken(claims, 8 * 60 * 60L);
    }

    /**
     * 用账号默认租户、不带 domainCode 签发 JWT 的便捷重载. 等价于 issueToken(account, null, null).
     *
     * @param account 账号实体
     * @return 8 小时过期的 JWT 字符串
     */
    private String issueToken(AccountDO account) {
        return issueToken(account, null, null);
    }

    /**
     * 注册请求体
     */
    public static class RegisterRequest {
        private String email;
        private String password;
        private String username;
        private String nickname;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }

    /**
     * 发送验证码请求体
     */
    public static class SendCodeRequest {
        private String receiver;
        private String channel;
        private String scene;

        public String getReceiver() {
            return receiver;
        }

        public void setReceiver(String receiver) {
            this.receiver = receiver;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getScene() {
            return scene;
        }

        public void setScene(String scene) {
            this.scene = scene;
        }
    }

    /**
     * 手机验证码登录请求体
     */
    public static class PhoneLoginRequest {
        private String phone;
        private String code;
        // 全局 Jackson 是 SNAKE_CASE (z-team JacksonConfig), @JsonAlias 兼容 camelCase 入参
        @JsonAlias("tenantCode")
        private String tenantCode;
        @JsonAlias("domainCode")
        private String domainCode;
        private String source;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getTenantCode() {
            return tenantCode;
        }

        public void setTenantCode(String tenantCode) {
            this.tenantCode = tenantCode;
        }

        public String getDomainCode() {
            return domainCode;
        }

        public void setDomainCode(String domainCode) {
            this.domainCode = domainCode;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    /**
     * 手机号注册请求体
     */
    public static class PhoneRegisterRequest {
        private String phone;
        private String code;
        private String password;
        private String username;
        private String nickname;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }

    /**
     * 找回密码请求体 (验证码 + 新密码)
     */
    public static class ResetPasswordByCodeRequest {
        private String receiver;
        private String channel;
        private String code;
        // 全局 Jackson 是 SNAKE_CASE, @JsonAlias 兼容 camelCase 入参 (newPassword)
        @JsonAlias("newPassword")
        private String newPassword;

        public String getReceiver() {
            return receiver;
        }

        public void setReceiver(String receiver) {
            this.receiver = receiver;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    private static class LoginAttempt {
        final int count;
        final long windowStartedAt;

        LoginAttempt(int count, long windowStartedAt) {
            this.count = count;
            this.windowStartedAt = windowStartedAt;
        }
    }
}
