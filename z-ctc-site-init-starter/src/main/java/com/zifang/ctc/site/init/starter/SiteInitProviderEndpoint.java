package com.zifang.ctc.site.init.starter;

import com.zifang.ctc.common.spi.site.SiteInitInvokeRequest;
import com.zifang.ctc.common.spi.site.SiteInitInvokeResult;
import com.zifang.ctc.common.spi.site.SiteInitManifest;
import com.zifang.ctc.common.spi.site.SiteInitProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标准协议 · 域侧端点: ctc 按这两个端点感知并调用各域的建站初始化能力.
 *
 * <ul>
 *   <li>GET  /api/site-init/{domainCode}/manifest → SiteInitManifest (该域能替站点做哪些初始化)</li>
 *   <li>POST /api/site-init/{domainCode}/invoke   → SiteInitInvokeResult (执行)</li>
 * </ul>
 *
 * <p>按 domainCode 分路径的原因: 一个宿主 JVM 可以同时宿主多个域 (如 z-opc-main 同时挂
 * z-kb-web 与 z-meta-web), 各域共用同一组端点路径会撞映射; 聚合端点在宿主内按域分发.
 * 只做转发: 业务逻辑在域自己的 {@link SiteInitProvider} 实现里.
 */
@RestController
@RequestMapping("/api/site-init")
public class SiteInitProviderEndpoint {

    private final List<SiteInitProvider> providers;

    public SiteInitProviderEndpoint(List<SiteInitProvider> providers) {
        this.providers = providers;
    }

    @GetMapping("/{domainCode}/manifest")
    public SiteInitManifest manifest(@PathVariable("domainCode") String domainCode) {
        SiteInitProvider provider = providerOf(domainCode);
        if (provider == null) {
            return null;
        }
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

    @PostMapping("/{domainCode}/invoke")
    public SiteInitInvokeResult invoke(@PathVariable("domainCode") String domainCode,
                                       @RequestBody SiteInitInvokeRequest request) {
        SiteInitProvider provider = providerOf(domainCode);
        if (provider == null) {
            return SiteInitInvokeResult.fail("域未注册: " + domainCode);
        }
        try {
            SiteInitInvokeResult result = provider.invoke(request);
            return result == null ? SiteInitInvokeResult.fail("域未返回结果") : result;
        } catch (Exception e) {
            return SiteInitInvokeResult.fail("执行异常: " + e.getMessage());
        }
    }

    private SiteInitProvider providerOf(String domainCode) {
        if (domainCode == null || providers == null) {
            return null;
        }
        for (SiteInitProvider p : providers) {
            if (domainCode.equals(p.domainCode())) {
                return p;
            }
        }
        return null;
    }
}
