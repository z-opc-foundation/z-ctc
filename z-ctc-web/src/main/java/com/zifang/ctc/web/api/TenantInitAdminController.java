package com.zifang.ctc.web.api;

import com.zifang.ctc.core.domain.entity.TenantDO;
import com.zifang.ctc.core.init.TenantInitContext;
import com.zifang.ctc.core.init.TenantInitOrchestrator;
import com.zifang.ctc.core.init.TenantInitReport;
import com.zifang.ctc.core.service.TenantService;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FEATURE050: 租户初始化管理端 endpoint.
 *
 * <p>典型用途:
 * <ul>
 *   <li>运维补数据: 已有租户缺 z-oss bucket / 缺 z-config 配置, 调这个重跑</li>
 *   <li>新加一个 Initializer 后, 给所有已有租户补一遍: 循环调 /reinit/{tenantCode}</li>
 *   <li>调试: 排查 init 流程, 看 Report</li>
 * </ul>
 *
 * <p>权限: TODO - 应该加 admin 角色校验. 目前只受 SSO 拦截, 登录即可访问.
 *
 * <p>端点:
 * <ul>
 *   <li>POST /api/ctc/admin/tenant-init/reinit/{tenantCode} - 重跑指定租户的 init</li>
 *   <li>POST /api/ctc/admin/tenant-init/reinit-all - 重跑所有租户 (运维批量)</li>
 *   <li>GET  /api/ctc/admin/tenant-init/status - 列出已注册的 Initializer</li>
 * </ul>
 */
@RestController("tenantInitAdminController")
@RequestMapping("/api/ctc/admin/tenant-init")
public class TenantInitAdminController {

    private static final Logger log = LogManager.getLogger(TenantInitAdminController.class);

    private final TenantService tenantService;
    private final TenantInitOrchestrator tenantInitOrchestrator;

    public TenantInitAdminController(TenantService tenantService, TenantInitOrchestrator tenantInitOrchestrator) {
        this.tenantService = tenantService;
        this.tenantInitOrchestrator = tenantInitOrchestrator;
    }

    /**
     * 重跑指定租户的所有 TenantInitializer. 幂等.
     */
    @PostMapping("/reinit/{tenantCode}")
    public Result<TenantInitReport> reinit(@PathVariable("tenantCode") String tenantCode) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return Result.<TenantInitReport>fail("tenantCode 必填").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        TenantDO t = tenantService.findByTenantCode(tenantCode).orElse(null);
        if (t == null) {
            return Result.<TenantInitReport>fail("租户不存在: " + tenantCode)
                    .code(ResultCode.NOT_FOUND.getCode());
        }
        log.info("[TenantInit-Admin] 手动重跑租户初始化: tenantCode={} adminPath=reinit", tenantCode);
        TenantInitContext ctx = TenantInitContext.of(t.getTenantCode(), t.getId(), null);
        TenantInitReport report = tenantInitOrchestrator.runAfterTenantCreated(ctx);
        return Result.success(report);
    }

    /**
     * 重跑所有租户. 运维场景: 加了新 Initializer 后批量补.
     *
     * <p>每个租户独立 try/catch, 一个失败不影响其他.
     */
    @PostMapping("/reinit-all")
    public Result<List<TenantInitReport>> reinitAll() {
        List<TenantDO> all = tenantService.listAll();
        log.info("[TenantInit-Admin] 手动重跑所有租户初始化: 总数={}", all.size());
        List<TenantInitReport> reports = all.stream().map(t -> {
            try {
                TenantInitContext ctx = TenantInitContext.of(t.getTenantCode(), t.getId(), null);
                return tenantInitOrchestrator.runAfterTenantCreated(ctx);
            } catch (Exception e) {
                log.error("[TenantInit-Admin] 租户 {} 重跑失败: {}", t.getTenantCode(), e.getMessage(), e);
                TenantInitReport r = new TenantInitReport(t.getTenantCode());
                r.addFailure("orchestrator", e);
                return r;
            }
        }).collect(Collectors.toList());

        long ok = reports.stream().filter(TenantInitReport::isAllSuccess).count();
        long fail = reports.size() - ok;
        log.info("[TenantInit-Admin] 批量重跑完成: 总={} 成功={} 失败={}", all.size(), ok, fail);
        return Result.success(reports);
    }

    /**
     * 查看已注册的 Initializer 列表 (用于排错 / 文档展示).
     */
    @org.springframework.web.bind.annotation.GetMapping("/status")
    public Result<List<InitializerInfo>> status() {
        List<InitializerInfo> list = tenantInitOrchestrator.listInitializers().stream()
                .map(i -> new InitializerInfo(i.moduleName(), i.order(), i.getClass().getSimpleName()))
                .collect(Collectors.toList());
        return Result.success(list);
    }

    /**
     * 简单的 VO, 避免直接暴露 TenantInitializer 接口 (里面 methodName 不必要)
     */
    public static class InitializerInfo {
        public final String module;
        public final int order;
        public final String className;

        public InitializerInfo(String module, int order, String className) {
            this.module = module;
            this.order = order;
            this.className = className;
        }
    }
}
