package com.zifang.ctc.site.init.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.common.spi.site.SiteInitCapability;
import com.zifang.ctc.common.spi.site.SiteInitManifest;
import com.zifang.ctc.common.spi.site.SiteInitProvider;
import com.zifang.ctc.common.spi.site.SiteInitRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 自登记器: 启动后把本域的建站初始化能力登记到 4A 中心.
 *
 * <p>退避重试: 域与 ctc 不保证同时起来, ctc 晚起时首次登记必然连接被拒;
 * 不重试会让本域永远缺席 ctc 的注册表 (只能重启补登记)。故按 5s/15s/30s/60s 退避重试,
 * 容器关闭即停。
 */
public class SiteInitRegistrar {

    private static final Logger log = LoggerFactory.getLogger(SiteInitRegistrar.class);

    /** 退避重试间隔 (毫秒) */
    private static final long[] BACKOFF_MS = {5_000L, 15_000L, 30_000L, 60_000L};

    private final SiteInitProvider provider;
    private final SiteInitProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean stopped = false;

    public SiteInitRegistrar(SiteInitProvider provider, SiteInitProperties properties) {
        this.provider = provider;
        this.properties = properties;
    }

    /** 停止重试 (容器关闭时调用) */
    public void stop() {
        stopped = true;
    }

    /**
     * 登记一次; 失败返回 false.
     */
    public boolean registerOnce() {
        String ctcBaseUrl = trimTrailingSlash(properties.getCtcBaseUrl());
        if (ctcBaseUrl == null || ctcBaseUrl.isEmpty()) {
            log.warn("[SiteInit] 未配置 z.ctc.site-init.ctc-base-url, 跳过登记");
            return false;
        }
        String endpoint = trimTrailingSlash(properties.getEndpoint());
        if (endpoint == null || endpoint.isEmpty()) {
            log.warn("[SiteInit] 未配置 z.ctc.site-init.endpoint, 跳过登记");
            return false;
        }
        SiteInitManifest manifest = provider.manifest();
        List<String> capabilities = new ArrayList<>();
        if (manifest != null && manifest.getCapabilities() != null) {
            for (SiteInitCapability c : manifest.getCapabilities()) {
                if (c != null && c.getCode() != null) {
                    capabilities.add(c.getCode());
                }
            }
        }
        SiteInitRegistration registration = new SiteInitRegistration();
        registration.setDomainCode(provider.domainCode());
        registration.setDomainName(provider.domainName());
        registration.setOrder(properties.getOrder() != null ? properties.getOrder() : provider.order());
        registration.setEndpoint(endpoint);
        registration.setProtocolVersion(SiteInitManifest.PROTOCOL_VERSION);
        registration.setCapabilities(capabilities);

        try {
            byte[] body = objectMapper.writeValueAsBytes(registration);
            HttpURLConnection conn = (HttpURLConnection) new URL(ctcBaseUrl + "/api/ctc/site-init/registrations").openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            if (properties.getRegisterToken() != null && !properties.getRegisterToken().isEmpty()) {
                conn.setRequestProperty("X-Site-Init-Token", properties.getRegisterToken());
            }
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body);
            }
            int code = conn.getResponseCode();
            conn.disconnect();
            if (code >= 200 && code < 300) {
                log.info("[SiteInit] 登记成功: domain={} endpoint={} capabilities={}",
                        provider.domainCode(), endpoint, capabilities);
                return true;
            }
            log.warn("[SiteInit] 登记失败: domain={} HTTP {}", provider.domainCode(), code);
            return false;
        } catch (Exception e) {
            log.warn("[SiteInit] 登记异常: domain={} err={}", provider.domainCode(), e.getMessage());
            return false;
        }
    }

    /**
     * 退避重试登记 (守护线程; 成功即退出).
     */
    public void registerWithRetry() {
        Thread t = new Thread(() -> {
            int attempt = 0;
            while (!stopped) {
                boolean ok = registerOnce();
                if (ok) {
                    return;
                }
                long wait = BACKOFF_MS[Math.min(attempt, BACKOFF_MS.length - 1)];
                attempt++;
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "site-init-registrar-" + provider.domainCode());
        t.setDaemon(true);
        t.start();
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
