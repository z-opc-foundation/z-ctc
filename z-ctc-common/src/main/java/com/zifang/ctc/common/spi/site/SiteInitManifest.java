package com.zifang.ctc.common.spi.site;

import java.util.ArrayList;
import java.util.List;

/**
 * 域侧能力清单: 某域声明自己能替站点做哪些初始化.
 *
 * <p>标准协议端点 GET {baseUrl}/api/site-init/manifest 返回本对象.
 * ctc 拿它渲染「建站管理 → 初始化能力清单」, 并按 capability.code 发起调用.
 */
public class SiteInitManifest {

    /** 协议版本, 当前 "1.0"; 不匹配的注册 ctc 拒绝 (预留向前兼容) */
    public static final String PROTOCOL_VERSION = "1.0";

    /** 本清单所操协议版本 — manifest 自报, ctc 侧按此判断兼容性 */
    private String protocolVersion = PROTOCOL_VERSION;

    /** 域编码 (全平台唯一, 如 z-cognition) */
    private String domainCode;

    /** 域名称 (展示用) */
    private String domainName;

    /** 能力清单 */
    private List<SiteInitCapability> capabilities = new ArrayList<>();

    public SiteInitManifest() {
    }

    public SiteInitManifest(String domainCode, String domainName, List<SiteInitCapability> capabilities) {
        this.domainCode = domainCode;
        this.domainName = domainName;
        this.capabilities = capabilities == null ? new ArrayList<>() : capabilities;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
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

    public List<SiteInitCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<SiteInitCapability> capabilities) {
        this.capabilities = capabilities == null ? new ArrayList<>() : capabilities;
    }
}
