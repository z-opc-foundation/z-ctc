package com.zifang.ctc.core.init;

/**
 * 租户初始化器 SPI.
 *
 * <p>每个 z-* 业务模块可实现这个接口, 在新租户创建后由
 * {@link TenantInitOrchestrator} 收集并顺序调用, 完成该租户下的默认数据初始化
 * (菜单/角色/默认配置/OSS bucket/工作流模板 等).
 *
 * <p>实现要求:
 * <ul>
 *   <li>必须是无状态 Spring Bean (推荐 @Component, 单例)</li>
 *   <li>{@link #order()} 返回数字越小越先执行; 跨模块依赖通过 order 协调
 *       (例如 z-ctc 内置初始化应先于 z-config 业务数据, 推荐 0~100 给 z-ctc 内部用, 100+ 给业务模块)</li>
 *   <li>{@link #moduleName()} 返回实现所属模块 (如 "z-config", "z-trade"), 用于日志与排错</li>
 *   <li>实现应 try/catch 自己的异常, 不要抛出到 Orchestrator (除非整个流程必须 abort)</li>
 *   <li>幂等: 重复调用应安全; 实际新租户只会调一次, 但手工数据修复场景可能重跑</li>
 * </ul>
 *
 * <p>注册方式:
 * <ul>
 *   <li>实现类加 {@code @Component} (在 z-* 模块自己的包下)</li>
 *   <li>或通过 {@code META-INF/spring/...AutoConfiguration.imports} 注册 @Bean</li>
 *   <li>TenantInitOrchestrator 会在 Spring 启动时通过 ApplicationContext.getBeansOfType 收集</li>
 * </ul>
 */
public interface TenantInitializer {

    /**
     * 模块名, 用于日志. 例如 "z-ctc", "z-config".
     */
    String moduleName();

    /**
     * 顺序, 数字越小越先执行. 默认 1000.
     * - z-ctc 内置初始化 (default role/menu) 应在 0~100
     * - 业务模块建议 100~900, 同优先级之间用 10/20/30 步进
     * - 后置钩子 (依赖前置数据) 1000+
     */
    default int order() {
        return 1000;
    }

    /**
     * 新租户创建后被调用. 异常应内部处理或包装后抛出 (Orchestrator 会捕获并记录).
     *
     * @param ctx 新租户上下文, 不可为 null
     */
    void onTenantCreated(TenantInitContext ctx);
}
