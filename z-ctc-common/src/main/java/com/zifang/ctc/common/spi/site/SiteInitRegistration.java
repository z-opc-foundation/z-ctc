package com.zifang.ctc.common.spi.site;

import java.util.ArrayList;
import java.util.List;

/**
 * 域注册报文 (域 → ctc): 域把自己「能替站点初始化什么」登记到 4A 中心.
 *
 * <p>标准协议端点 POST /api/ctc/site-init/registrations 接收本对象 (幂等: 同 domainCode 覆盖更新).
 */
public class SiteInitRegistration {

    /** 域编码 (全平台唯一, 如 z-cognition) */
    private String domainCode;

    /** 域名称 (展示用) */
    private String domainName;

    /** 跨域执行顺序, 数字小者先跑; 同值按 domainCode 字典序 */
    private Integer order;

    /** 域的初始化服务根地址 (ctc 按标准协议调 {endpoint}/api/site-init/*) */
    private String endpoint;

    /** 协议版本, 不匹配 ctc 拒绝该注册 */
    private String protocolVersion = SiteInitManifest.PROTOCOL_VERSION;

    /** 能力编码清单 (取自 manifest.capabilities[].code) */
    private List<String> capabilities = new ArrayList<>();

    public SiteInitRegistration() {
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities == null ? new ArrayList<>() : capabilities;
    }
}
