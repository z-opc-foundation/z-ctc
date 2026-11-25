package com.zifang.ctc.core.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.common.spi.site.SiteInitInvokeRequest;
import com.zifang.ctc.common.spi.site.SiteInitInvokeResult;
import com.zifang.ctc.common.spi.site.SiteInitManifest;
import com.zifang.ctc.core.domain.entity.SiteInitRegistrationDO;
import com.zifang.ctc.core.domain.entity.SiteInitTaskDO;
import com.zifang.ctc.core.service.SiteInitService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 建站初始化编排器: ctc 感知到的域注册表 → 按顺序调各域的标准协议 → 落任务记录.
 *
 * <p>与既有 {@link TenantInitOrchestrator} 的分工:
 * <ul>
 *   <li>TenantInitOrchestrator: 同进程 Spring Bean 的初始化 (z-ctc 自带的默认域/默认组织)</li>
 *   <li>SiteInitOrchestrator: 跨进程注册域的初始化 (各域实现接口并注册到 ctc)</li>
 * </ul>
 * 建站时两者都跑: 本地先 (域/组织是别的域初始化的前置), 远程后.
 */
@Component
public class SiteInitOrchestrator {

    private static final Logger log = LogManager.getLogger(SiteInitOrchestrator.class);

    private final SiteInitService siteInitService;
    private final RemoteSiteInitClient remoteClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 双构造器时 Spring 6 不会自动选定, 只找默认构造 ⇒ 启动即崩 (No default constructor found);
    // 必须显式指认注入入口, RemoteSiteInitClient 走源码 new, 不是 bean.
    @Autowired
    public SiteInitOrchestrator(SiteInitService siteInitService) {
        this(siteInitService, new RemoteSiteInitClient());
    }

    public SiteInitOrchestrator(SiteInitService siteInitService, RemoteSiteInitClient remoteClient) {
        this.siteInitService = siteInitService;
        this.remoteClient = remoteClient;
    }

    /**
     * 对一个站点跑全量域初始化.
     *
     * @param tenantCode 站点(租户)编码
     * @param tenantId   站点主键 (可空)
     * @param params     传给各域的公共参数 (可空)
     * @param operator   操作者 (可空)
     * @param traceId    链路追踪 id (空则自动生成)
     * @return 汇总结果
     */
    public SiteInitRunResult run(String tenantCode, Long tenantId, Map<String, Object> params,
                                  String operator, String traceId) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            throw new IllegalArgumentException("tenantCode 必填");
        }
        String trace = (traceId == null || traceId.isEmpty()) ? UUID.randomUUID().toString() : traceId;
        List<SiteInitRegistrationDO> registrations = siteInitService.listActive();
        log.info("[SiteInit] 开始建站初始化: tenantCode={} 注册域={} traceId={}", tenantCode, registrations.size(), trace);

        SiteInitRunResult result = new SiteInitRunResult(tenantCode);
        for (SiteInitRegistrationDO reg : registrations) {
            long t0 = System.currentTimeMillis();
            try {
                SiteInitInvokeRequest req = new SiteInitInvokeRequest();
                req.setTenantCode(tenantCode);
                req.setTenantId(tenantId);
                req.setParams(params);
                req.setTraceId(trace);
                req.setOperator(operator);
                req.setIdempotencyKey(tenantCode + ":" + reg.getDomainCode() + ":" + trace);

                String requestJson = objectMapper.writeValueAsString(req);
                SiteInitTaskDO task = siteInitService.startTask(tenantCode, reg.getDomainCode(), null,
                        req.getIdempotencyKey(), requestJson, trace);

                SiteInitInvokeResult invokeResult = remoteClient.invoke(reg.getEndpoint(), reg.getDomainCode(), req);
                long cost = System.currentTimeMillis() - t0;
                String responseJson = objectMapper.writeValueAsString(invokeResult);
                int status = invokeResult.isSuccess() ? SiteInitTaskDO.STATUS_SUCCESS : SiteInitTaskDO.STATUS_FAILED;
                siteInitService.finishTask(task.getId(), status, responseJson, invokeResult.getMessage(), cost);

                result.addDomain(reg, invokeResult.isSuccess(), invokeResult.getMessage(), cost);
                log.info("[SiteInit]   ✓ domain={} cost={}ms msg={}", reg.getDomainCode(), cost, invokeResult.getMessage());
            } catch (Exception e) {
                long cost = System.currentTimeMillis() - t0;
                log.warn("[SiteInit]   ✗ domain={} 编排异常: {}", reg.getDomainCode(), e.getMessage(), e);
                result.addDomain(reg, false, "编排异常: " + e.getMessage(), cost);
            }
        }
        log.info("[SiteInit] 建站初始化结束: tenantCode={} 成功={} 失败={} 耗时={}ms",
                tenantCode, result.getSuccessCount(), result.getFailureCount(), result.getElapsedMs());
        return result;
    }

    /**
     * 只跑一个域 (建站管理页上「单独重跑某域」用).
     */
    public SiteInitRunResult runDomain(String tenantCode, Long tenantId, String domainCode,
                                        Map<String, Object> params, String operator, String traceId) {
        if (domainCode == null || domainCode.isEmpty()) {
            throw new IllegalArgumentException("domainCode 必填");
        }
        SiteInitRegistrationDO reg = null;
        for (SiteInitRegistrationDO r : siteInitService.listActive()) {
            if (domainCode.equals(r.getDomainCode())) {
                reg = r;
                break;
            }
        }
        SiteInitRunResult result = new SiteInitRunResult(tenantCode);
        if (reg == null) {
            SiteInitRegistrationDO missing = new SiteInitRegistrationDO();
            missing.setDomainCode(domainCode);
            result.addDomain(missing, false, "域未注册或已停用", 0);
            return result;
        }
        String trace = (traceId == null || traceId.isEmpty()) ? UUID.randomUUID().toString() : traceId;
        long t0 = System.currentTimeMillis();
        try {
            SiteInitInvokeRequest req = new SiteInitInvokeRequest();
            req.setTenantCode(tenantCode);
            req.setTenantId(tenantId);
            req.setParams(params);
            req.setTraceId(trace);
            req.setOperator(operator);
            req.setIdempotencyKey(tenantCode + ":" + domainCode + ":" + trace);
            String requestJson = objectMapper.writeValueAsString(req);
            SiteInitTaskDO task = siteInitService.startTask(tenantCode, domainCode, null,
                    req.getIdempotencyKey(), requestJson, trace);
            SiteInitInvokeResult invokeResult = remoteClient.invoke(reg.getEndpoint(), domainCode, req);
            long cost = System.currentTimeMillis() - t0;
            siteInitService.finishTask(task.getId(),
                    invokeResult.isSuccess() ? SiteInitTaskDO.STATUS_SUCCESS : SiteInitTaskDO.STATUS_FAILED,
                    objectMapper.writeValueAsString(invokeResult), invokeResult.getMessage(), cost);
            result.addDomain(reg, invokeResult.isSuccess(), invokeResult.getMessage(), cost);
        } catch (Exception e) {
            result.addDomain(reg, false, "编排异常: " + e.getMessage(), System.currentTimeMillis() - t0);
        }
        return result;
    }

    /**
     * 拉某注册域的能力清单 (建站管理页展示用; 拉不到返回空清单).
     */
    public SiteInitManifest manifestOf(String domainCode) {
        for (SiteInitRegistrationDO r : siteInitService.listAll()) {
            if (domainCode.equals(r.getDomainCode())) {
                SiteInitManifest m = remoteClient.fetchManifest(r.getEndpoint(), domainCode);
                return m == null ? new SiteInitManifest(r.getDomainCode(), r.getDomainName(), null) : m;
            }
        }
        return null;
    }
}
