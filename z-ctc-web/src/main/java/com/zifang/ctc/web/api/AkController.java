package com.zifang.ctc.web.api;

import com.zifang.ctc.core.ak.AkCaller;
import com.zifang.ctc.core.ak.AkException;
import com.zifang.ctc.core.ak.AkService;
import com.zifang.ctc.core.ak.model.AkCallLogDetail;
import com.zifang.ctc.core.ak.model.AkCallLogInput;
import com.zifang.ctc.core.ak.model.AkCallLogPage;
import com.zifang.ctc.core.ak.model.AkCallLogQuery;
import com.zifang.ctc.core.ak.model.AkIdentity;
import com.zifang.ctc.core.ak.model.AkPayload;
import com.zifang.ctc.core.ak.model.AkRecord;
import com.zifang.ctc.core.ak.model.AkScope;
import com.zifang.ctc.core.ak.model.AkScopeDomain;
import com.zifang.ctc.core.ak.model.AkScopeSaveRequest;
import com.zifang.util.core.meta.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AK 台账 REST 面（认证中心持有 AK 真源）。
 *
 * <p>业务服务（z-kb 等）不再自己存 AK：签发、验签、scope 授权、调用日志都在这里，
 * 业务侧通过 HTTP 调用过来。明文 secret 只在签发那一次响应里返回，库里只存哈希。</p>
 */
@Tag(name = "AK 台账")
@RestController
@RequestMapping("/api/ctc/v1/ak")
public class AkController {

    private final AkService akService;
    private final AkCallerResolver callerResolver;

    public AkController(AkService akService, AkCallerResolver callerResolver) {
        this.akService = akService;
        this.callerResolver = callerResolver;
    }

    @Operation(summary = "我的 AK 列表")
    @PostMapping("/list")
    public Result<List<AkRecord>> list(@RequestBody(required = false) AkPayload body,
            HttpServletRequest request) {
        return Result.success(akService.list(body, caller(request)));
    }

    @Operation(summary = "租户内 AK 列表（管理视角）")
    @PostMapping("/admin/list")
    public Result<List<AkRecord>> adminList(@RequestBody(required = false) AkPayload body,
            HttpServletRequest request) {
        return Result.success(akService.adminList(body, caller(request)));
    }

    @Operation(summary = "客户端 AK 列表")
    @PostMapping("/client/list")
    public Result<List<AkRecord>> clientList(@RequestBody(required = false) AkPayload body,
            HttpServletRequest request) {
        return Result.success(akService.clientList(body, caller(request)));
    }

    @Operation(summary = "签发 AK（明文 secret 仅本次响应返回）")
    @PostMapping("/create")
    public Result<AkRecord> create(@RequestBody AkPayload body, HttpServletRequest request) {
        return Result.success(akService.create(body, caller(request)));
    }

    @Operation(summary = "客户端签发 AK")
    @PostMapping("/client/create")
    public Result<AkRecord> clientCreate(@RequestBody AkPayload body, HttpServletRequest request) {
        return Result.success(akService.create(body, caller(request)));
    }

    @Operation(summary = "验签（明文 secret 换身份）")
    @PostMapping("/verify")
    public Result<AkIdentity> verify(@RequestBody VerifyRequest body) {
        String candidate = body == null ? null : body.getSecret();
        if (candidate == null && body != null) {
            candidate = body.getAkCode();
        }
        try {
            return Result.success(akService.verify(candidate));
        } catch (AkException ex) {
            return Result.<AkIdentity>fail(ex.getMessage()).code(ex.getCode());
        }
    }

    @Operation(summary = "AK 通行判定（验签 + scope 授权一步到位，业务服务的保护链路走这里）")
    @PostMapping("/gate")
    public Result<Map<String, Object>> gate(@RequestBody GateRequest body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            AkIdentity identity = akService.verify(body == null ? null : body.getSecret());
            boolean allowed = akService.authorize(identity,
                    body == null ? null : body.getDomain(),
                    body == null ? null : body.getAction(),
                    body == null ? null : body.getResource());
            result.put("allowed", Boolean.valueOf(allowed));
            result.put("reason", allowed ? "ALLOWED" : "DENIED");
            result.put("identity", identity);
            return Result.success(result);
        } catch (AkException ex) {
            result.put("allowed", Boolean.FALSE);
            result.put("reason", "INVALID");
            result.put("code", Integer.valueOf(ex.getCode()));
            result.put("message", ex.getMessage());
            return Result.success(result);
        }
    }

    @Operation(summary = "scope 目录")
    @PostMapping("/scope/tree")
    public Result<List<AkScopeDomain>> scopeTree(HttpServletRequest request) {
        return Result.success(akService.tree(caller(request)));
    }

    @Operation(summary = "某个 AK 的 scope 明细")
    @PostMapping("/scope/detail")
    public Result<Map<String, List<AkScope>>> scopeDetail(@RequestBody(required = false) DetailRequest body,
            HttpServletRequest request) {
        AkCaller caller = caller(request);
        return Result.success(akService.detail(body == null ? null : body.getAkCode(), caller, true));
    }

    @Operation(summary = "保存 scope（覆盖写）")
    @PostMapping("/scope/save")
    public Result<Void> scopeSave(@RequestBody AkScopeSaveRequest body, HttpServletRequest request) {
        try {
            akService.save(body, caller(request));
            return Result.success();
        } catch (AkException ex) {
            return Result.<Void>fail(ex.getMessage()).code(ex.getCode());
        }
    }

    @Operation(summary = "调用日志分页")
    @PostMapping("/call-log/list")
    public Result<AkCallLogPage> callLogList(@RequestBody(required = false) AkCallLogQuery query,
            HttpServletRequest request) {
        try {
            return Result.success(akService.callLogs(query, caller(request)));
        } catch (AkException ex) {
            return Result.<AkCallLogPage>fail(ex.getMessage()).code(ex.getCode());
        }
    }

    @Operation(summary = "调用日志详情")
    @PostMapping("/call-log/detail")
    public Result<AkCallLogDetail> callLogDetail(@RequestBody(required = false) DetailRequest body,
            HttpServletRequest request) {
        try {
            AkCaller caller = caller(request);
            if (body == null) {
                return Result.success(akService.callLogDetail(null, null, caller));
            }
            return Result.success(body.getAkCode() != null && !body.getAkCode().isBlank()
                    ? akService.callLogDetail(body.getAkCode(), body.getRecordId(), caller)
                    : akService.callLogDetailByAkId(body.getAkId(), body.getRecordId(), caller));
        } catch (AkException ex) {
            return Result.<AkCallLogDetail>fail(ex.getMessage()).code(ex.getCode());
        }
    }

    @Operation(summary = "记一条调用日志（业务服务的保护链路上报）")
    @PostMapping("/call-log/record")
    public Result<Boolean> callLogRecord(@RequestBody(required = false) RecordRequest body) {
        if (body == null || body.getInput() == null) {
            return Result.success(Boolean.FALSE);
        }
        try {
            akService.recordCall(body.getIdentity(), body.getInput());
            return Result.success(Boolean.TRUE);
        } catch (RuntimeException ex) {
            // 日志旁路失败不改业务结果
            return Result.success(Boolean.FALSE);
        }
    }

    private AkCaller caller(HttpServletRequest request) {
        return callerResolver.resolve(request);
    }

    /** 验签入参（兼容历史 akCode 字段：只当待验 secret 用）。 */
    public static class VerifyRequest {
        private String secret;
        private String akCode;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getAkCode() { return akCode; }
        public void setAkCode(String akCode) { this.akCode = akCode; }
    }

    /** 通行判定入参。 */
    public static class GateRequest {
        private String secret;
        private String domain;
        private String action;
        private String resource;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
    }

    /** 明细入参。 */
    public static class DetailRequest {
        private String akCode;
        private String akId;
        private String recordId;

        public String getAkCode() { return akCode; }
        public void setAkCode(String akCode) { this.akCode = akCode; }
        public String getAkId() { return akId; }
        public void setAkId(String akId) { this.akId = akId; }
        public String getRecordId() { return recordId; }
        public void setRecordId(String recordId) { this.recordId = recordId; }
    }

    /** 调用日志上报入参。 */
    public static class RecordRequest {
        private AkIdentity identity;
        private AkCallLogInput input;

        public AkIdentity getIdentity() { return identity; }
        public void setIdentity(AkIdentity identity) { this.identity = identity; }
        public AkCallLogInput getInput() { return input; }
        public void setInput(AkCallLogInput input) { this.input = input; }
    }
}
