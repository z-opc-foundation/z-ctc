package com.zifang.ctc.client.starter;

import com.zifang.ctc.client.ZCtcClient;
import com.zifang.ctc.client.ZCtcClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 把 {@link ZCtcClient} 装配成 bean —— 本 starter 的全部职责。
 *
 * <p>刻意不注册成 {@code @ComponentScan} 扫出来：那些 bean 会随本 jar 路径被卷进
 * 任何宿主的扫描根，正是 all-in-one 时期各板块实现互相污染的成因。
 * 走 spring.factories 显式注册 + {@code @ConditionalOnProperty}，
 * 「不配 z.ctc.base-url 就不存在这个 bean」，边界清楚。
 */
@Configuration
@EnableConfigurationProperties(ZCtcClientProperties.class)
@ConditionalOnProperty(prefix = "z.ctc", name = "base-url")
public class ZCtcClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ZCtcClient zCtcClient(ZCtcClientProperties properties) {
        return ZCtcClientFactory.remote(properties.getBaseUrl());
    }
}
