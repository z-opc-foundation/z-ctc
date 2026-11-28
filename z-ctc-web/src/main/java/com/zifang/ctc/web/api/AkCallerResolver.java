package com.zifang.ctc.web.api;

import com.zifang.ctc.core.ak.AkCaller;
import com.zifang.ctc.core.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * AK 域调用人解析。
 *
 * <p>可信来源优先级：登录态（{@link TenantContext}）＞ 服务间透传头。
 * 服务间调用（业务服务 → 认证中心）走透传头，AK 台账按该租户 / 操作人归属。</p>
 *
 * @author zifang
 */
@Component
public class AkCallerResolver {

    /** 解析调用人。 */
    public AkCaller resolve(HttpServletRequest request) {
        String tenantCode = header(request, "X-Tenant-Code");
        String actorId = header(request, "X-Actor-Id");
        // HTTP 头只允许 ASCII：kb 侧对中文姓名做百分号编码，这里解回（兼容历史裸 ASCII 值）
        String actorName = decodeHeaderValue(header(request, "X-Actor-Name"));
        if (TenantContext.getTenantCode() != null && !TenantContext.getTenantCode().isEmpty()) {
            tenantCode = TenantContext.getTenantCode();
            if (TenantContext.getUserId() != null) {
                actorId = String.valueOf(TenantContext.getUserId());
            }
        }
        return new AkCaller(tenantCode, actorId, actorName);
    }

    private String header(HttpServletRequest request, String name) {
        if (request == null) {
            return null;
        }
        String value = request.getHeader(name);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String decodeHeaderValue(String value) {
        if (value == null || !value.contains("%")) {
            return value;
        }
        try {
            return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
