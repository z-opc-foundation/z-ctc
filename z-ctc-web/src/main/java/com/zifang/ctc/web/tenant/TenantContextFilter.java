package com.zifang.ctc.web.tenant;

import com.zifang.ctc.core.tenant.TenantContext;
import com.zifang.ctc.sso.model.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 租户上下文 Filter (FEATURE049, 放在 z-ctc-web 模块).
 * <p>
 * 每个请求进来时:
 * 1. SsoInterceptor 已把 UserInfo 写入 request attribute "ssoUser"
 * 2. 本 Filter 读 UserInfo, 提取 userId/tenantCode/domainCode/roleCode
 * 3. 写入 TenantContext (ThreadLocal)
 * 4. 请求结束后清理 (避免线程复用泄露)
 *
 * <h3>优先级</h3>
 * 设 {@code HIGHEST_PRECEDENCE + 20}, 在 SsoInterceptor 之后, Controller 之前. 因为
 * SsoInterceptor 是 Spring MVC 的 HandlerInterceptor (preHandle 在 DispatcherServlet 里跑),
 * 而本 Filter 是 Servlet Filter, 在 DispatcherServlet 之前. 所以需要高优先级
 * 让 SsoInterceptor 先跑.
 *
 * <p>实际: SsoInterceptor 在 DispatcherServlet 阶段填 "ssoUser", 本 Filter 在那之前就跑了,
 * 此时 request.getAttribute("ssoUser") 是 null. 因此本 Filter 不能依赖这个 attribute.
 * 改为: 自己解析 Authorization 头 (JwtUtil 在 z-ctc-sso 里有 Spring Bean).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(TenantContextFilter.class);

    private final com.zifang.ctc.sso.JwtUtil jwtUtil;

    public TenantContextFilter(com.zifang.ctc.sso.JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            TenantContext.TenantInfo info = resolveTenantInfo(request);
            if (info != null) {
                TenantContext.set(info);
            }
            chain.doFilter(request, response);
        } finally {
            // 清理 ThreadLocal, 防止线程复用泄露
            TenantContext.clear();
        }
    }

    /**
     * 从 Authorization 头解析 token, 拿到租户信息.
     * 解析失败或无 token 时返回 null (业务层按需降级).
     */
    private TenantContext.TenantInfo resolveTenantInfo(HttpServletRequest request) {
        // 兜底: SsoInterceptor 可能已经写入 "ssoUser" (同请求内复用)
        Object existing = request.getAttribute("ssoUser");
        if (existing instanceof UserInfo) {
            UserInfo ui = (UserInfo) existing;
            return toTenantInfo(ui);
        }
        // 主路径: 解析 Authorization 头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (token.isEmpty()) {
            return null;
        }

        try {
            com.zifang.ctc.sso.JwtUtil.VerificationResult v = jwtUtil.verifyToken(token);
            if (!v.isValid()) {
                return null;
            }
            java.util.Map<String, Object> claims = v.getClaims();
            if (claims == null) {
                return null;
            }


            Long userId = parseLong(claims.get("userId"));
            if (userId == null) {
                return null;
            }

            String tenantCode = (String) claims.get("tenantCode");
            String domainCode = (String) claims.get("domainCode");
            String roleCode = claims.get("accountType") != null
                    ? String.valueOf(claims.get("accountType"))
                    : null;
            return TenantContext.TenantInfo.of(userId, tenantCode, domainCode, roleCode);
        } catch (Exception e) {
            log.debug("TenantContextFilter 解析 token 失败: {}", e.getMessage());
            return null;
        }
    }

    private TenantContext.TenantInfo toTenantInfo(UserInfo ui) {
        if (ui.getUserId() == null) {
            return null;
        }

        Long userId;
        try {
            userId = Long.parseLong(ui.getUserId());
        } catch (NumberFormatException e) {
            return null;
        }
        String roleCode = ui.getRoles() != null && !ui.getRoles().isEmpty()
                ? ui.getRoles().iterator().next()
                : null;
        return TenantContext.TenantInfo.of(userId, ui.getTenantCode(), ui.getDomainCode(), roleCode);
    }

    private Long parseLong(Object o) {
        if (o == null) {
            return null;
        }

        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
