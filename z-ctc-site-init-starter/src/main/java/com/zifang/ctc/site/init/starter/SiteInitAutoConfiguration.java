package com.zifang.ctc.site.init.starter;

import com.zifang.ctc.common.spi.site.SiteInitProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 建站初始化协议 · 域侧装配.
 *
 * <p>不配 {@code z.ctc.site-init.enabled=true} 就什么都不装: 既不暴露端点也不登记。
 * 域只需提供 {@link SiteInitProvider} 的 bean, 端点与自登记由本装配类接管。
 */
@Configuration
@EnableConfigurationProperties(SiteInitProperties.class)
@ConditionalOnProperty(prefix = "z.ctc.site-init", name = "enabled", havingValue = "true")
public class SiteInitAutoConfiguration {

    /**
     * 标准协议端点 (域侧暴露能力清单与执行入口).
     */
    @Bean
    @ConditionalOnMissingBean
    public SiteInitProviderEndpoint siteInitProviderEndpoint(SiteInitProvider provider) {
        return new SiteInitProviderEndpoint(provider);
    }

    /**
     * 自登记器: 启动后把本域登记到 4A 中心, 失败退避重试.
     */
    @Bean
    @ConditionalOnMissingBean
    public SiteInitRegistrar siteInitRegistrar(SiteInitProvider provider, SiteInitProperties properties) {
        SiteInitRegistrar registrar = new SiteInitRegistrar(provider, properties);
        registrar.registerWithRetry();
        return registrar;
    }
}
