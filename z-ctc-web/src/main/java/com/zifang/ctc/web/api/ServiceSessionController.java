package com.zifang.ctc.web.api;

import com.zifang.ctc.core.service.session.ServiceSession;
import com.zifang.ctc.core.service.session.ServiceSessionService;
import com.zifang.util.core.meta.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 机器 / 服务账号会话 REST 面（认证中心持有令牌真源）。
 *
 * <p>设备控制台一类的机器访问不再自建令牌表：签发 / 验签 / 吊销都在这里，
 * 业务域的守卫只做一次转发校验。令牌明文只在签发那一次响应返回，库里只存哈希。</p>
 */
@Tag(name = "机器 / 服务账号会话")
@RestController
@RequestMapping("/api/ctc/v1/service-session")
public class ServiceSessionController {

    private final ServiceSessionService sessionService;

    public ServiceSessionController(ServiceSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(summary = "签发机器 / 服务账号令牌（明文仅本次响应返回）")
    @PostMapping("/issue")
    public Result<ServiceSession> issue(@RequestBody IssueRequest body) {
        if (body == null || body.getSubjectCode() == null || body.getSubjectCode().trim().isEmpty()) {
            return Result.<ServiceSession>fail("主体标识不能为空").code(40001);
        }
        ServiceSession session = sessionService.issue(body.getTenantCode(), body.getSubjectType(),
                body.getSubjectCode(), body.getTtlMillis());
        return Result.success(session);
    }

    @Operation(summary = "验签：令牌换会话身份")
    @PostMapping("/verify")
    public Result<Map<String, Object>> verify(@RequestBody VerifyRequest body) {
        ServiceSession session = body == null ? null : sessionService.verify(body.getToken());
        Map<String, Object> data = new LinkedHashMap<>();
        if (session == null) {
            data.put("valid", Boolean.FALSE);
            return Result.success(data);
        }
        data.put("valid", Boolean.TRUE);
        data.put("tenantCode", session.getTenantCode());
        data.put("subjectType", session.getSubjectType());
        data.put("subjectCode", session.getSubjectCode());
        data.put("issuedAt", Long.valueOf(session.getIssuedAt()));
        data.put("expiresAt", Long.valueOf(session.getExpiresAt()));
        return Result.success(data);
    }

    @Operation(summary = "吊销令牌")
    @PostMapping("/revoke")
    public Result<Boolean> revoke(@RequestBody VerifyRequest body) {
        boolean revoked = body != null && sessionService.revoke(body.getToken());
        return Result.success(Boolean.valueOf(revoked));
    }

    @Operation(summary = "有效会话数（按租户）")
    @PostMapping("/active-count")
    public Result<Map<String, Object>> activeCount(@RequestBody(required = false) CountRequest body) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activeCount", Integer.valueOf(
                sessionService.activeCount(body == null ? null : body.getTenantCode())));
        return Result.success(data);
    }

    /** 会话数入参。 */
    public static class CountRequest {
        private String tenantCode;

        public String getTenantCode() { return tenantCode; }
        public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    }

    /** 签发入参。 */
    public static class IssueRequest {
        private String tenantCode;
        private String subjectType;
        private String subjectCode;
        private long ttlMillis;

        public String getTenantCode() { return tenantCode; }
        public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
        public String getSubjectType() { return subjectType; }
        public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public long getTtlMillis() { return ttlMillis; }
        public void setTtlMillis(long ttlMillis) { this.ttlMillis = ttlMillis; }
    }

    /** 验签 / 吊销入参。 */
    public static class VerifyRequest {
        private String token;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
