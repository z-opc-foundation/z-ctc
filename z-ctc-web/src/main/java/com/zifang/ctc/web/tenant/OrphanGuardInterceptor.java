package com.zifang.ctc.web.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.core.tenant.TenantContext;
import com.zifang.util.core.meta.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import com.zifang.util.core.json.JsonMapperFactory;

/**
 * 游离态守卫拦截器 (FEATURE049).
 * <p>
 * 当 {@link TenantContext#isOrphan()} 为 true (用户已登录但 0 个 active membership) 时,
 * 限制其只能访问白名单接口:
 * <ul>
 *   <li>GET  /api/ctc/authn/whoami — 查自己身份</li>
 *   <li>GET  /api/ctc/authn/my-tenants — 查自己租户列表</li>
 *   <li>POST /api/ctc/authn/select-tenant — 选租户</li>
 *   <li>POST /api/ctc/authn/switch-tenant — 切换租户 (游离态也可清空)</li>
 *   <li>POST /api/ctc/ac/tenants — 创建租户</li>
 *   <li>POST /api/ctc/ac/invitations/accept — 接受邀请</li>
 *   <li>POST /api/ctc/ac/invitations/redeem — 兑换邀请码</li>
 *   <li>/api/ctc/authn/register — 注册 (但已登录就不该走)</li>
 * </ul>
 * 其他业务接口 (task / trade / order / asset 等) 一律拒绝 (403).
 *
 * <h3>位置</h3>
 * 注册在 SsoInterceptor 之后, Controller 之前. Spring MVC HandlerInterceptor.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class OrphanGuardInterceptor implements HandlerInterceptor {

    private static final Logger log = LogManager.getLogger(OrphanGuardInterceptor.class);

    /**
     * 游离态白名单 — 不加 tenant 也能访问的路径
     */
    private static final List<String> ORPHAN_WHITELIST = Arrays.asList(
            "/api/ctc/authn/whoami",
            "/api/ctc/authn/my-tenants",
            "/api/ctc/authn/select-tenant",
            "/api/ctc/authn/switch-tenant",
            "/api/ctc/authn/register",
            "/api/ctc/authn/login",
            "/api/ctc/authn/verify",
            // 验证码体系 (注册 / 手机验证码登录 / 找回密码) — 未选租户前可用
            "/api/ctc/authn/send-code",
            "/api/ctc/authn/phone-login",
            "/api/ctc/authn/register-phone",
            "/api/ctc/authn/reset-password",
            "/api/ctc/ac/tenants",
            "/api/ctc/ac/tenants/**",
            "/api/ctc/ac/invitations/accept",
            "/api/ctc/ac/invitations/redeem",
            // 静态 / 文档 / 健康检查
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/error"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = JsonMapperFactory.getDefault();
    /** 去掉 context-path 前缀, 取相对应用路径. 部署在 /z-team 等非根 context-path 时,
     * getRequestURI() 返回 /z-team/api/ctc/authn/whoami 会与白名单 /api/ctc/authn/whoami
     * 失配导致全部 403, 必须用 getPathWithinApplication 保证路径与白名单对齐. */
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 未登录: 不在游离态守卫范围内, 让 SsoInterceptor 自行处理
        if (!TenantContext.isOrphan()) {
            return true;
        }
        // 用相对应用路径 (去掉 context-path) 与白名单匹配, 避免部署在 /z-team 等非根路径时全部失配
        String uri = urlPathHelper.getPathWithinApplication(request);
        // 白名单放行
        for (String pattern : ORPHAN_WHITELIST) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        // OPTIONS 放行 (CORS 预检)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 拒绝
        log.warn("OrphanGuardInterceptor 拒绝游离态访问业务接口: userId={}, uri={}",
                TenantContext.getUserId(), uri);
        writeForbidden(response, "游离态用户 (0 membership) 不能访问该接口, 请先创建租户或接受邀请");
        return false;
    }

    private void writeForbidden(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Result<Void> body = Result.<Void>fail(msg).code(403);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
