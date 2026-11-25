package com.zifang.ctc.web.api;

import com.zifang.ctc.common.spi.site.SiteInitManifest;
import com.zifang.ctc.core.domain.entity.SiteInitTaskDO;
import com.zifang.ctc.core.init.SiteInitOrchestrator;
import com.zifang.ctc.core.init.SiteInitRunResult;
import com.zifang.ctc.core.service.PlatformAdminService;
import com.zifang.ctc.core.service.SiteInitService;
import com.zifang.ctc.core.service.TenantService;
import com.zifang.ctc.core.domain.entity.TenantDO;
import com.zifang.ctc.core.tenant.TenantContext;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 建站初始化 · 编排端点 (ctc 侧).
 *
 * <p>端点:
 * <ul>
 *   <li>POST /api/ctc/site-init/runs          - 对某站点跑全量域初始化</li>
 *   <li>POST /api/ctc/site-init/runs/domain   - 只跑某域 (单域重跑)</li>
 *   <li>GET  /api/ctc/site-init/runs/page     - 任务记录分页</li>
 *   <li>GET  /api/ctc/site-init/runs/{id}     - 任务详情</li>
 *   <li>GET  /api/ctc/site-init/manifest/{domainCode} - 某注册域的能力清单</li>
 * </ul>
 */
@RestController("siteInitRunController")
@RequestMapping("/api/ctc/site-init")
public class SiteInitRunController {

    private final SiteInitOrchestrator orchestrator;
    private final SiteInitService siteInitService;
    private final TenantService tenantService;
    private final PlatformAdminService platformAdminService;

    public SiteInitRunController(SiteInitOrchestrator orchestrator, SiteInitService siteInitService,
                                 TenantService tenantService, PlatformAdminService platformAdminService) {
        this.orchestrator = orchestrator;
        this.siteInitService = siteInitService;
        this.tenantService = tenantService;
        this.platformAdminService = platformAdminService;
    }

    /**
     * 触发建站初始化 (全量域).
     */
    @PostMapping("/runs")
    @BizApi(summary = "对某站点执行建站初始化 (全量注册域)", description = "对某站点执行建站初始化 (全量注册域).", tags = { "z-ctc" })
    public Result<SiteInitRunResult> run(
            @RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "请求体 { tenantCode, params }", position = BizParam.Position.BODY) Map<String, Object> body) {
        if (!isPlatformAdmin()) {
            return Result.<SiteInitRunResult>fail("需要平台管理员权限").code(ResultCode.REQ_REJECT.getCode());
        }
        String tenantCode = str(body, "tenantCode");
        if (tenantCode == null || tenantCode.isEmpty()) {
            return Result.<SiteInitRunResult>fail("tenantCode 必填").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        TenantDO tenant = tenantService.findByTenantCode(tenantCode).orElse(null);
        if (tenant == null) {
            return Result.<SiteInitRunResult>fail("站点不存在: " + tenantCode).code(ResultCode.NOT_FOUND.getCode());
        }
        Map<String, Object> params = paramsOf(body);
        SiteInitRunResult result = orchestrator.run(tenantCode, tenant.getId(), params,
                String.valueOf(TenantContext.getUserId()), null);
        return Result.success(result);
    }

    /**
     * 只跑某域 (单域重跑).
     */
    @PostMapping("/runs/domain")
    @BizApi(summary = "对某站点只执行某个域的建站初始化", description = "对某站点只执行某个域的建站初始化.", tags = { "z-ctc" })
    public Result<SiteInitRunResult> runDomain(
            @RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "请求体 { tenantCode, domainCode, params }", position = BizParam.Position.BODY) Map<String, Object> body) {
        if (!isPlatformAdmin()) {
            return Result.<SiteInitRunResult>fail("需要平台管理员权限").code(ResultCode.REQ_REJECT.getCode());
        }
        String tenantCode = str(body, "tenantCode");
        String domainCode = str(body, "domainCode");
        if (tenantCode == null || tenantCode.isEmpty() || domainCode == null || domainCode.isEmpty()) {
            return Result.<SiteInitRunResult>fail("tenantCode / domainCode 必填").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        TenantDO tenant = tenantService.findByTenantCode(tenantCode).orElse(null);
        Long tenantId = tenant == null ? null : tenant.getId();
        SiteInitRunResult result = orchestrator.runDomain(tenantCode, tenantId, domainCode, paramsOf(body),
                String.valueOf(TenantContext.getUserId()), null);
        return Result.success(result);
    }

    /**
     * 任务记录分页.
     */
    @GetMapping("/runs/page")
    @BizApi(summary = "建站初始化任务记录分页", description = "建站初始化任务记录分页.", tags = { "z-ctc" })
    public Result<Map<String, Object>> page(
            @RequestParam(value = "tenantCode", required = false)
            @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "站点编码, 可选", position = BizParam.Position.QUERY) String tenantCode,
            @RequestParam(value = "domainCode", required = false)
            @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 可选", position = BizParam.Position.QUERY) String domainCode,
            @RequestParam(value = "pageNum", defaultValue = "1")
            @BizParam(name = "pageNum", type = BizField.FieldType.INTEGER, description = "页码", position = BizParam.Position.QUERY) int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20")
            @BizParam(name = "pageSize", type = BizField.FieldType.INTEGER, description = "每页大小", position = BizParam.Position.QUERY) int pageSize) {
        if (!isPlatformAdmin()) {
            return Result.<Map<String, Object>>fail("需要平台管理员权限").code(ResultCode.REQ_REJECT.getCode());
        }
        long[] total = new long[1];
        List<SiteInitTaskDO> rows = siteInitService.pageTasks(tenantCode, domainCode, pageNum, pageSize, total);
        Map<String, Object> data = new HashMap<>();
        data.put("data", rows);
        data.put("total", total[0]);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return Result.success(data);
    }

    /**
     * 某注册域的能力清单 (实时从域侧拉).
     */
    @GetMapping("/manifest/{domainCode}")
    @BizApi(summary = "拉取某注册域的能力清单", description = "拉取某注册域的能力清单.", tags = { "z-ctc" })
    public Result<SiteInitManifest> manifest(@PathVariable("domainCode")
    @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码", position = BizParam.Position.PATH) String domainCode) {
        if (!isPlatformAdmin()) {
            return Result.<SiteInitManifest>fail("需要平台管理员权限").code(ResultCode.REQ_REJECT.getCode());
        }
        SiteInitManifest manifest = orchestrator.manifestOf(domainCode);
        if (manifest == null) {
            return Result.<SiteInitManifest>fail("域未注册: " + domainCode).code(ResultCode.NOT_FOUND.getCode());
        }
        return Result.success(manifest);
    }

    private boolean isPlatformAdmin() {
        return platformAdminService.isPlatformAdmin(TenantContext.getUserId());
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> paramsOf(Map<String, Object> body) {
        if (body == null) {
            return new HashMap<>();
        }
        Object v = body.get("params");
        return v instanceof Map ? (Map<String, Object>) v : new HashMap<>();
    }

}
