package com.zifang.ctc.client.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code z.ctc.*} 配置。语义与 z-ctc-sso 的 {@code sso.ctc-base-url} 一致：
 * 同一个 4A 中心地址在两处各写一次，是因为两者是两条独立链路
 * （登录态验签 / 读授权数据），不该互相依赖。
 */
@ConfigurationProperties(prefix = "z.ctc")
public class ZCtcClientProperties {

    /**
     * 4A 中心 base URL，例如 {@code http://z-ctc.z-ctc.svc:8888}。
     * 留空则不注册 {@code ZCtcClient} bean（消费方自己用裸 SDK 或另有实现）。
     */
    private String baseUrl = "";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.trim().isEmpty();
    }
}
