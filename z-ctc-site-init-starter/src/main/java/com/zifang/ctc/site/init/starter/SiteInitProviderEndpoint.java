package com.zifang.ctc.site.init.starter;

import com.zifang.ctc.common.spi.site.SiteInitInvokeRequest;
import com.zifang.ctc.common.spi.site.SiteInitInvokeResult;
import com.zifang.ctc.common.spi.site.SiteInitManifest;
import com.zifang.ctc.common.spi.site.SiteInitProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准协议 · 域侧端点: ctc 按这两个端点感知并调用本域的建站初始化能力.
 *
 * <ul>
 *   <li>GET  /api/site-init/manifest → SiteInitManifest (本域能替站点做哪些初始化)</li>
 *   <li>POST /api/site-init/invoke   → SiteInitInvokeResult (执行)</li>
 * </ul>
 *
 * <p>只做转发: 业务逻辑在域自己的 {@link SiteInitProvider} 实现里.
 */
@RestController
@RequestMapping("/api/site-init")
public class SiteInitProviderEndpoint {

    private final SiteInitProvider provider;

    public SiteInitProviderEndpoint(SiteInitProvider provider) {
        this.provider = provider;
    }

    @GetMapping("/manifest")
    public SiteInitManifest manifest() {
        SiteInitManifest manifest = provider.manifest();
        if (manifest != null && (manifest.getDomainCode() == null || manifest.getDomainCode().isEmpty())) {
            manifest.setDomainCode(provider.domainCode());
        }
        if (manifest != null && (manifest.getDomainName() == null || manifest.getDomainName().isEmpty())) {
            manifest.setDomainName(provider.domainName());
        }
        if (manifest != null && (manifest.getProtocolVersion() == null || manifest.getProtocolVersion().isEmpty())) {
            manifest.setProtocolVersion(SiteInitManifest.PROTOCOL_VERSION);
        }
        return manifest;
    }

    @PostMapping("/invoke")
    public SiteInitInvokeResult invoke(@RequestBody SiteInitInvokeRequest request) {
        try {
            SiteInitInvokeResult result = provider.invoke(request);
            return result == null ? SiteInitInvokeResult.fail("域未返回结果") : result;
        } catch (Exception e) {
            return SiteInitInvokeResult.fail("执行异常: " + e.getMessage());
        }
    }
}
