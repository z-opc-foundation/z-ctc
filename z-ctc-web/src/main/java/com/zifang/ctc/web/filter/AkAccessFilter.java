package com.zifang.ctc.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.core.ak.model.AkIdentity;
import com.zifang.ctc.core.ak.AkService;
import com.zifang.util.core.meta.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AK 保护链路：验签 + 默认拒绝 scope + 可信租户请求头覆盖 + 旁路脱敏日志。
 *
 * <p>放在认证中心这边，业务服务内嵌本过滤器时即由 z-ctc 统一判定 AK；
 * 受保护路径前缀用 {@code ctc.ak.protect-paths} 配，授权域用 {@code ctc.ak.domain} 配
 * （缺省 SYSTEM —— 未声明的域不放行）。</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class AkAccessFilter extends OncePerRequestFilter {

    private final AkService accessService;
    private final String[] protectPaths;
    private final String domain;
    private final ObjectMapper json = new ObjectMapper();

    public AkAccessFilter(AkService accessService, String[] protectPaths, String domain) {
        this.accessService = accessService;
        this.protectPaths = protectPaths == null ? new String[0] : protectPaths;
        this.domain = domain == null || domain.trim().isEmpty() ? "SYSTEM" : domain.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : protectPaths) {
            if (prefix != null && !prefix.isEmpty() && path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        long started = System.currentTimeMillis();
        AkIdentity identity;
        try {
            identity = accessService.verify(request.getHeader("X-AK"));
        } catch (RuntimeException ex) {
            reject(response, 401, 51301, "AK 无效");
            return;
        }
        if (!accessService.authorize(identity, domain, "CALL", request.getRequestURI())) {
            reject(response, 403, 51303, "AK scope 拒绝访问");
            return;
        }
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, 65536);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        com.zifang.ctc.core.ak.model.AkCallLogInput input = new com.zifang.ctc.core.ak.model.AkCallLogInput();
        input.setHttpMethod(request.getMethod());
        input.setRequestPath(request.getRequestURI());
        input.setQuery(request.getQueryString());
        input.setRequestHeaders(headers(request));
        Exception failure = null;
        try {
            chain.doFilter(new TrustedCallerRequest(cachedRequest, identity), cachedResponse);
        } catch (Exception ex) {
            failure = ex;
            throw ex;
        } finally {
            input.setResponseBody(text(cachedResponse.getContentAsByteArray()));
            input.setRequestBody(text(cachedRequest.getContentAsByteArray()));
            input.setHttpStatus(cachedResponse.getStatus());
            input.setOutcome(failure != null ? "INTERRUPTED"
                    : cachedResponse.getStatus() >= 400 ? "HTTP_FAILURE" : "SUCCESS");
            input.setDurationMs(System.currentTimeMillis() - started);
            try {
                accessService.recordCall(identity, input);
            } catch (RuntimeException ex) {
                // 日志旁路失败不能改变已完成的业务响应。
            } finally {
                cachedResponse.copyBodyToResponse();
            }
        }
    }

    private static Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            result.put(name, request.getHeader(name));
        }
        return result;
    }

    private static String text(byte[] bytes) {
        return bytes == null || bytes.length == 0 ? "" : new String(bytes, StandardCharsets.UTF_8);
    }

    private void reject(HttpServletResponse response, int status, int code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json.writeValueAsString(Result.fail(message).code(code)));
    }

    /** 仅覆盖网关头；AK 绑定的租户/归属人优先于任何客户端自报值。 */
    private static final class TrustedCallerRequest extends HttpServletRequestWrapper {
        private final AkIdentity identity;

        TrustedCallerRequest(HttpServletRequest request, AkIdentity identity) {
            super(request);
            this.identity = identity;
        }

        @Override
        public String getHeader(String name) {
            if ("X-Tenant-Code".equalsIgnoreCase(name)) {
                return identity.getTenantCode();
            }
            if ("X-Actor-Id".equalsIgnoreCase(name)) {
                return identity.getOwnerId();
            }
            if ("X-Actor-Name".equalsIgnoreCase(name)) {
                return identity.getOwnerName();
            }
            return super.getHeader(name);
        }
    }
}
