package com.zifang.ctc.site.init.starter;

import com.zifang.ctc.common.spi.site.SiteInitProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 建站初始化协议 · 域侧装配.
 *
 * <p>不配 {@code z.ctc.site-init.enabled=true} 就什么都不装: 既不暴露端点也不登记。
 * 域只需提供 {@link SiteInitProvider} 的 bean (一个宿主 JVM 可以挂多个域的 provider),
 * 端点与自登记由本装配类接管 —— 端点按 domainCode 分路径在宿主内分发。
 */
@Configuration
@EnableConfigurationProperties(SiteInitProperties.class)
@ConditionalOnProperty(prefix = "z.ctc.site-init", name = "enabled", havingValue = "true")
public class SiteInitAutoConfiguration {

    /**
     * 标准协议端点 (域侧暴露能力清单与执行入口, 按 domainCode 分发).
     */
    @Bean
    @ConditionalOnMissingBean
    public SiteInitProviderEndpoint siteInitProviderEndpoint(ObjectProvider<SiteInitProvider> providers) {
        return new SiteInitProviderEndpoint(providers.orderedStream().toList());
    }

    /**
     * 自登记器: 启动后把宿主内各域逐个登记到 4A 中心, 失败退避重试.
     */
    @Bean
    @ConditionalOnMissingBean
    public SiteInitRegistrar siteInitRegistrar(ObjectProvider<SiteInitProvider> providers, SiteInitProperties properties) {
        List<SiteInitProvider> list = providers.orderedStream().toList();
        if (list.isEmpty()) {
            throw new IllegalStateException(
                    "z.ctc.site-init.enabled=true 但宿主内没有任何 SiteInitProvider bean —— 请先实现协议接口");
        }
        SiteInitRegistrar registrar = new SiteInitRegistrar(list, properties);
        registrar.registerWithRetry();
        return registrar;
    }
}
