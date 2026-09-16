package com.zifang.ctc.core.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 租户初始化编排器. 在新租户创建后, 收集所有 {@link TenantInitializer} 并按 order 顺序执行.
 *
 * <p>设计原则:
 * <ul>
 *   <li>解耦: 各模块独立实现 {@link TenantInitializer}, 不需要 z-ctc 知道具体业务</li>
 *   <li>容错: 单个 Initializer 失败不阻塞整体; 记录 WARN 日志, 流程继续</li>
 *   <li>可观察: 启动时打印 "已注册 N 个 TenantInitializer" + 排序后列表</li>
 *   <li>可扩展: 后续可加 retry / 异步 / 事务 等策略, 不影响调用方</li>
 * </ul>
 *
 * <p>调用流程:
 * <pre>
 *   TenantController.create() →
 *     tenantService.createTenant() → DB INSERT
 *     membershipService.createOwner() (FEATURE049)
 *     tenantInitOrchestrator.runAfterTenantCreated(ctx) → 收集 + 排序 + 逐个调
 * </pre>
 *
 * <p>手动触发: 也可单独调用 runAfterTenantCreated(tenantCode) 用于运维补数据场景
 * (例如已有租户缺 z-oss bucket, 管理员跑一遍补齐).
 */
@Component
public class TenantInitOrchestrator {

    private static final Logger log = LogManager.getLogger(TenantInitOrchestrator.class);

    private final ObjectProvider<TenantInitializer> initializerProvider;

    /**
     * 启动时缓存的已排序列表. 业务模块注册新 Initializer 后需重启才能生效.
     */
    private List<TenantInitializer> cachedInitializers = new ArrayList<>();

    public TenantInitOrchestrator(ObjectProvider<TenantInitializer> initializerProvider) {
        this.initializerProvider = initializerProvider;
    }

    /**
     * Spring 启动完毕后收集并排序所有 Initializer, 缓存备用.
     * 缓存避免每次创建租户都重新查 ApplicationContext, 也方便启动日志看到清单.
     */
    @PostConstruct
    public void warmup() {
        List<TenantInitializer> all = new ArrayList<>();
        initializerProvider.forEach(all::add);
        all.sort(Comparator.comparingInt(TenantInitializer::order).thenComparing(TenantInitializer::moduleName));
        this.cachedInitializers = all;

        log.info("[TenantInit] 已注册 {} 个 TenantInitializer:", all.size());
        for (TenantInitializer i : all) {
            log.info("[TenantInit]   - order={} module={} class={}",
                    i.order(), i.moduleName(), i.getClass().getSimpleName());
        }
    }

    /**
     * 新租户创建后调用. 顺序执行所有 Initializer, 单个失败不阻塞.
     *
     * @param ctx 上下文, 不可为 null
     * @return 报告 (成功 N 个 / 失败 M 个), 便于调用方记录/展示
     */
    public TenantInitReport runAfterTenantCreated(TenantInitContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("TenantInitContext 不可为 null");
        }
        if (ctx.tenantCode() == null || ctx.tenantCode().isEmpty()) {
            throw new IllegalArgumentException("TenantInitContext.tenantCode 必填");
        }

        log.info("[TenantInit] 开始初始化新租户: tenantCode={} tenantId={} createdBy={}",
                ctx.tenantCode(), ctx.tenantId(), ctx.createdBy());

        TenantInitReport report = new TenantInitReport(ctx.tenantCode());

        for (TenantInitializer init : cachedInitializers) {
            String m = init.moduleName();
            long t0 = System.currentTimeMillis();
            try {
                init.onTenantCreated(ctx);
                long cost = System.currentTimeMillis() - t0;
                report.addSuccess(m, cost);
                log.info("[TenantInit]   ✓ {} order={} 耗时={}ms", m, init.order(), cost);
            } catch (Throwable t) {
                long cost = System.currentTimeMillis() - t0;
                report.addFailure(m, t);
                // 不抛出, 记录 WARN; 整个租户创建流程不回滚
                log.warn("[TenantInit]   ✗ {} order={} 耗时={}ms 失败: {}",
                        m, init.order(), cost, t.getMessage(), t);
            }
        }

        log.info("[TenantInit] 租户初始化完成: tenantCode={} 成功={} 失败={} 总耗时={}ms",
                ctx.tenantCode(), report.getSuccessCount(), report.getFailureCount(), report.getTotalCostMs());

        return report;
    }

    /**
     * 当前已注册的 Initializer 数量, 用于测试/管理端展示.
     */
    public int getInitializerCount() {
        return cachedInitializers.size();
    }

    /**
     * 列出当前已注册的 Initializer (按 order 排序), 用于管理端展示/调试.
     */
    public List<TenantInitializer> listInitializers() {
        return new ArrayList<>(cachedInitializers);
    }
}
