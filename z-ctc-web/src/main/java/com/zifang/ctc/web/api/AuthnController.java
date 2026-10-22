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
import com.zifang.ctc.sso.config.SsoInterceptor;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

    /**
     * 卡 TASK-20261022-027: 自助认证端点（send-code / register / phone-login / register-phone）
     * <b>设计上必须免登录</b> ⇒ 收不到 SSO 闸门的保护 ⇒ 只能用<b>频率限制</b>收敛滥用面。
     * 下面是限额的<b>默认值</b>（可被环境变量覆盖，见 z-ctc-admin 的 {@code z.ctc.authn.*}）。
     */
    private static final long MINUTE_MILLIS = 60_000L;
    private static final long HOUR_MILLIS = 60 * 60 * 1000L;

    /** 邮箱格式（宽松：只要求「有 @ 且 @ 后面还有一段」）。 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final AuthService authService;
    private final LoginLogMapper loginLogMapper;
    private final Cache<String, LoginAttempt> loginAttempts = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100_000L)
            .build();

    /**
     * 自助认证端点的配额计数窗口（发码 / 改密）。
     * <p>过期时间取 2 小时，覆盖最长的一小时窗口；窗口过期后计数自然归零。</p>
     */
    private final Cache<String, QuotaWindow> authnQuotas = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .maximumSize(200_000L)
            .build();

    // ⚠ 字段初始值 = @Value 里的默认值: 这样**不启 Spring 容器**的单元测试
    //   拿到的也是合理限额（否则 int 默认 0 ⇒ 一切都判超限）。
    @Value("${z.ctc.authn.send-code.max-per-minute-per-ip:10}")
    private int maxSendCodePerMinutePerIp = 10;

    @Value("${z.ctc.authn.send-code.max-per-hour-per-receiver:5}")
    private int maxSendCodePerHourPerReceiver = 5;

    @Value("${z.ctc.authn.reset-password.max-per-hour-per-receiver:10}")
    private int maxResetPasswordPerHourPerReceiver = 10;

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
                                         HttpServletRequest httpRequest,
                                         HttpServletResponse response,
                                         @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        // ⚠ 客户端 IP **不再只从 `X-Client-Ip` 请求头取**（卡 TASK-20261022-001 第 2 项）：
        //   没有任何组件注入那个头时，取值**恒为 NULL**（实测 1863 行登录日志 100% 为 NULL）
        //   ⇒ 「字段存在」不等于「能按 IP 归因」。
        //   现在取**服务端看到的真实来源**：对端地址 + XFF 链，**以最后一跳可信地址为准**
        //   （口径与实现见 `SsoInterceptor.resolveClientIp`）。
        String clientIp = SsoInterceptor.resolveClientIp(httpRequest);
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
     * <p>
     * ⚠ 卡 TASK-20261022-027:
     * <ul>
     *   <li><b>第 3 项 · 补验证</b>: 改前只要 {@code email} + {@code password}(≥6 位) 就能建号
     *       ⇒ 匿名可批量建号。现在**必须带邮箱验证码**（先调 {@code /api/ctc/authn/send-code},
     *       {@code channel=EMAIL, scene=REGISTER}; 验证码一次性消费）。</li>
     *   <li><b>第 4 项 · 收口签发</b>: 改前注册成功**无条件签发一枚 SSO 闸门认可的 token**
     *       ⇒ 匿名 → 零凭据 → 合法登录态。现在**不再签发 token**（{@code token} 字段为空）:
     *       自助注册拿到的登录态必须由 {@code /api/ctc/authn/login} 正常登录产生
     *       （密码凭证的 identifier 就是注册用的邮箱, 见 {@code UserServiceImpl.register}）。</li>
     * </ul>
     */
    @Operation(summary = "注册 (FEATURE049, 创建游离 user)")
    @PostMapping("/register")
    public Result<RegisterResult> register(@RequestBody RegisterRequest req) {
        try {
            if (req == null || req.getEmail() == null || req.getPassword() == null) {
                return Result.<RegisterResult>fail("email / password 必填").code(400);
            }
            String email = req.getEmail().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                return Result.<RegisterResult>fail("邮箱格式不正确").code(400);
            }
            if (req.getPassword().length() < 6) {
                return Result.<RegisterResult>fail("密码至少 6 位").code(400);
            }
            // 卡 TASK-20261022-027 第 3 项: 邮箱验证码是**必填项**（不是可选加固）。
            if (req.getCode() == null || req.getCode().trim().isEmpty()) {
                return Result.<RegisterResult>fail(
                        "验证码必填 (先调用 /api/ctc/authn/send-code, channel=EMAIL, scene=REGISTER)").code(400);
            }
            try {
                verifyCodeService.verifyCode(email, VerifyCodeService.CHANNEL_EMAIL,
                        VerifyCodeService.SCENE_REGISTER, req.getCode().trim());
            } catch (IllegalArgumentException e) {
                return Result.<RegisterResult>fail(e.getMessage()).code(400);
            }
            // 委托给 UserService.register (含 BCrypt 哈希 + 查重 + 写 user + 写 auth)
            Long userId;
            try {
                userId = userService.register(email, req.getUsername(), req.getNickname(), req.getPassword());
            } catch (IllegalStateException e) {
                // 邮箱已被注册
                return Result.<RegisterResult>fail(e.getMessage()).code(409);
            } catch (IllegalArgumentException e) {
                return Result.<RegisterResult>fail(e.getMessage()).code(400);
            }

            // 兜底: 兼容期同步写一条 account (4A 老接口仍依赖 z_ctc_ac_account)
            // TODO: 等所有 Controller 切到 UserService 后删掉这段
            createAccountFallback(userId, req.getPassword());

            UserDO user = userService.findById(userId).orElseThrow(() -> new IllegalStateException("注册后 user 丢失"));

            RegisterResult body = new RegisterResult();
            body.setUserId(user.getId());
            body.setEmail(user.getEmail());
            body.setUsername(user.getUsername());
            body.setNickname(user.getNickname());
            // ⚠ 卡 TASK-20261022-027 第 4 项: **不签发 token**（留空）。
            //   自助注册**不得**无条件拿到一枚「SSO 闸门认可」的登录态 ——
            //   否则「匿名 → 零凭据 → 合法登录态 → 闸门被绕过」这条链又回来了。
            //   用户接着调 /api/ctc/authn/login (identifier=注册邮箱) 走正常登录即可。
            body.setToken(null);
            body.setOrphan(true);
            log.info("AuthnController.register 成功: userId={}, email={} (不签发 token, 请走 /api/ctc/authn/login)",
                    userId, maskForLog(email));
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
     * <p>
     * ⚠ 卡 TASK-20261022-027 第 3 项: 本端点<b>设计上必须免登录</b>（注册 / 手机登录 / 改密都要靠它）
     * ⇒ 收不到 SSO 闸门的保护 ⇒ <b>必须自带频率限制</b>，否则可被用来对任意手机号/邮箱狂发码。
     * 两层配额：
     * <ul>
     *   <li><b>按客户端 IP</b>（每分钟 N 次）: 挡住「一个来源狂刷」的批量滥用；</li>
     *   <li><b>按接收方</b>（每小时 N 次，<b>跨 scene 计数</b>）: 挡住「换 scene 绕过 60s 冷却」
     *       对同一个手机号反复发码。⚠ 这一层只在**真的要发码**时消耗（预检不通过不消耗），
     *       免得被人用「查未注册邮箱」的探测请求把受害者的额度打光。</li>
     * </ul>
     */
    @Operation(summary = "发送验证码 (注册/登录/找回密码)")
    @PostMapping("/send-code")
    public Result<Map<String, Object>> sendCode(@RequestBody SendCodeRequest req,
                                                HttpServletRequest httpRequest) {
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
        if (VerifyCodeService.CHANNEL_EMAIL.equals(channel) && !EMAIL_PATTERN.matcher(receiver).matches()) {
            return Result.<Map<String, Object>>fail("邮箱格式不正确").code(400);
        }

        // ① 客户端 IP 维度: 每分钟最多 N 次（对任意号码狂发码的总闸门）
        String clientIp = quotaClientIp(httpRequest);
        if (!acquireQuota("send-code:ip:" + clientIp, maxSendCodePerMinutePerIp, MINUTE_MILLIS)) {
            log.warn("AuthnController.sendCode 限流(按IP): ip={}, channel={}, scene={}", clientIp, channel, scene);
            return Result.<Map<String, Object>>fail("发送过于频繁, 请稍后再试").code(429);
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

        // ② 接收方维度: 每小时最多 N 次（跨 scene 计数）—— 只在真的要发码时消耗
        if (!acquireQuota("send-code:receiver:" + channel + ":" + receiver,
                maxSendCodePerHourPerReceiver, HOUR_MILLIS)) {
            log.warn("AuthnController.sendCode 限流(按接收方): channel={}, scene={}, receiver={}",
                    channel, scene, maskForLog(receiver));
            return Result.<Map<String, Object>>fail("该接收方获取验证码过于频繁, 请稍后再试").code(429);
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
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse response,
                                              @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        // 与密码登录同源：客户端 IP 取**真实来源**（对端地址 + XFF 链），不再依赖 `X-Client-Ip` 头。
        String clientIp = SsoInterceptor.resolveClientIp(httpRequest);
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
     * 手机号 + 验证码注册. 与邮箱注册同构: 创建游离 user + account 兼容兑底.
     * <p>
     * ⚠ 卡 TASK-20261022-027 第 4 项: 与邮箱注册一致，<b>不再签发 token</b>
     * （改前无条件签发一枚 SSO 闸门认可的 token ⇒ 零凭据登录态）。
     * 手机号注册**本来就要求验证码**，所以第 3 项在邮箱注册侧补齐即可。
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
            // ⚠ 卡 TASK-20261022-027 第 4 项: **不签发 token**（留空），同 #register。
            body.setToken(null);
            body.setOrphan(true);
            log.info("AuthnController.registerPhone 成功: userId={}, phone={} (不签发 token, 请走 /api/ctc/authn/login)",
                    userId, maskForLog(phone));
            return Result.success(body);
        } catch (Exception ex) {
            log.error("AuthnController.registerPhone 失败: {}", ex.getMessage(), ex);
            return Result.<RegisterResult>fail("register failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()).code(500);
        }
    }

    /**
     * 找回密码: 手机/邮箱验证码 + 新密码重置 (user + auth + account 三表同步).
     * <p>
     * ⚠ 卡 TASK-20261022-027: 本端点<b>刻意不在</b> {@code sso.exclude-paths} 里
     * （匿名可改密 = 账号接管面）⇒ 匿名请求在 SSO 闸门就被 401 挡掉；
     * 已登录用户改密仍可用，本方法再叠<b>验证码 + 频率限制</b>两层。
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
        // 频率限制: 同一接收方每小时最多 N 次改密尝试（验证码本身还有 5 次错误即作废）
        if (!acquireQuota("reset-password:receiver:" + channel + ":" + receiver,
                maxResetPasswordPerHourPerReceiver, HOUR_MILLIS)) {
            log.warn("AuthnController.resetPassword 限流: channel={}, receiver={}",
                    channel, maskForLog(receiver));
            return Result.<Void>fail("改密尝试过于频繁, 请稍后再试").code(429);
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
     * 原子消耗一次配额（卡 TASK-20261022-027 的频率限制）。
     *
     * <p><b>为什么需要</b>：{@code send-code} / {@code register} / {@code phone-login} /
     * {@code register-phone} 这几个端点<b>设计上必须免登录</b>（否则用户登不进去 / 注册不了）
     * ⇒ 收不到 SSO 闸门的保护 ⇒ 只能靠这里的频率限制收敛两个滥用面：
     * 「对任意手机号/邮箱狂发码」与「批量零凭据建号」。</p>
     *
     * @param key          配额键（调用方决定维度：客户端 IP / 接收方 …）
     * @param limit        窗口内允许的最大次数
     * @param windowMillis 计数窗口长度
     * @return 本次调用是否还在配额内（false ⇒ 已被限流，调用方应回 429）
     */
    private boolean acquireQuota(String key, int limit, long windowMillis) {
        long now = System.currentTimeMillis();
        QuotaWindow window = authnQuotas.asMap().compute(key, (k, current) -> {
            if (current == null || now - current.windowStartedAt >= windowMillis) {
                return new QuotaWindow(1, now);
            }
            return new QuotaWindow(current.count + 1, current.windowStartedAt);
        });
        return window.count <= limit;
    }

    /**
     * 取本次请求的客户端 IP（口径与登录日志一致：服务端对端地址 + XFF 链最后一跳可信地址）。
     * 取不到时退化成 "unknown" —— 仍然计数（宁可误伤也不放过匿名滥用）。
     */
    private static String quotaClientIp(HttpServletRequest httpRequest) {
        String ip = SsoInterceptor.resolveClientIp(httpRequest);
        return (ip == null || ip.trim().isEmpty()) ? "unknown" : ip.trim();
    }

    /**
     * 日志脱敏（卡 TASK-20261022-027）：邮箱 / 手机号属 PII，**不落明文进日志**。
     * 只保留首尾各一个字符，中间打码。
     */
    private static String maskForLog(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() <= 2) {
            return "**";
        }
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }

    /**
     * 记录登录审计日志. 异常被吞掉, 写日志失败不影响登录主流程.
     *
     * @param account       登录涉及的账号, 可为 null (账号不存在场景); 为 null 时 accountId 字段写空
     * @param identifier    登录标识 (用户名/手机号/邮箱 等)
     * @param identityType  凭证类型, 1=密码
     * @param status        登录状态, 1=成功, 0=失败
     * @param failureReason 失败原因, 成功时为 null
     * @param clientIp      客户端真实来源 IP（**服务端对端地址 + XFF 链，以最后一跳可信地址为准**；
     *                      取值口径见 {@link SsoInterceptor#resolveClientIp}）——
     *                      ⚠ 不再依赖 {@code X-Client-Ip} 请求头（没有组件注入它时恒为 NULL）
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
     * <p>
     * ⚠ 卡 TASK-20261022-027: 新增 {@code code}（邮箱验证码）—— 改前只要 email + password
     * 就能建号（零凭据自助注册）。验证码由 {@code /api/ctc/authn/send-code}
     * （channel=EMAIL, scene=REGISTER）下发，一次性消费。
     * </p>
     */
    public static class RegisterRequest {
        private String email;
        private String password;
        private String username;
        private String nickname;
        /** 邮箱验证码（scene=REGISTER）。 */
        private String code;

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

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
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

    /**
     * 配额计数窗口（卡 TASK-20261022-027）：与 {@link LoginAttempt} 同构，只是窗口更长。
     */
    private static class QuotaWindow {
        final int count;
        final long windowStartedAt;

        QuotaWindow(int count, long windowStartedAt) {
            this.count = count;
            this.windowStartedAt = windowStartedAt;
        }
    }
}
