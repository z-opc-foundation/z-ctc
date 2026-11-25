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
 *       order: 200                            # 跨域执行顺序, 数字小者先跑
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

    /** 跨域执行顺序, 数字小者先跑 */
    private Integer order = 200;

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
