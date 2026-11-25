package com.zifang.ctc.site.init.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 建站初始化协议配置 (域侧).
 *
 * <p>典型配置:
 * <pre>
 * z:
 *   ctc:
 *     site-init:
 *       enabled: true
 *       endpoint: http://z-cognition:8890     # 本域对外地址 (登记给 ctc, ctc 按此回调)
 *       ctc-base-url: http://z-ctc:8889      # 4A 中心地址
 *       register-token: xxx                   # 与 ctc 的 z.ctc.site-init.register-token 一致
 *       order: 200                            # 可选: 不配则听各域 provider 自己的 order()
 * </pre>
 */
@ConfigurationProperties(prefix = "z.ctc.site-init")
public class SiteInitProperties {

    /** 是否启用建站初始化协议 (默认关闭: 不配就不暴露端点也不登记) */
    private boolean enabled = false;

    /** 本域对外地址 (登记给 ctc, ctc 按 {endpoint}/api/site-init/* 回调) */
    private String endpoint;

    /** 4A 中心地址 */
    private String ctcBaseUrl;

    /** 服务间登记令牌 (与 ctc 侧一致) */
    private String registerToken;

    /**
     * 跨域执行顺序, 数字小者先跑.
     * <p>默认 null = 不干预, 听各域 provider 自己的 {@code order()} —— 多域宿主
     * (一个 JVM 挂多个 SiteInitProvider) 不配这项, 否则整宿主的域会被同一序号覆盖;
     * 单域部署想就地改序才配它。
     */
    private Integer order = null;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getCtcBaseUrl() {
        return ctcBaseUrl;
    }

    public void setCtcBaseUrl(String ctcBaseUrl) {
        this.ctcBaseUrl = ctcBaseUrl;
    }

    public String getRegisterToken() {
        return registerToken;
    }

    public void setRegisterToken(String registerToken) {
        this.registerToken = registerToken;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
