package com.zifang.ctc.core.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.common.spi.site.SiteInitInvokeRequest;
import com.zifang.ctc.common.spi.site.SiteInitInvokeResult;
import com.zifang.ctc.common.spi.site.SiteInitManifest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 远程域初始化客户端: 按标准协议调注册域.
 *
 * <p>标准协议:
 * <ul>
 *   <li>GET  {endpoint}/api/site-init/manifest → SiteInitManifest</li>
 *   <li>POST {endpoint}/api/site-init/invoke   → SiteInitInvokeResult</li>
 * </ul>
 *
 * <p>用 HttpURLConnection 而非 RestTemplate: core 模块不引 web 客户端栈, 与 z-ctc-client
 * 的裸 HTTP 模板同风格 (跨板块调用只留最小依赖面).
 */
public class RemoteSiteInitClient {

    private static final Logger log = LogManager.getLogger(RemoteSiteInitClient.class);

    /** 协议路径: 能力清单 */
    public static final String MANIFEST_PATH = "/api/site-init/manifest";
    /** 协议路径: 执行初始化 */
    public static final String INVOKE_PATH = "/api/site-init/invoke";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public RemoteSiteInitClient() {
        this(3000, 30000);
    }

    public RemoteSiteInitClient(int connectTimeoutMs, int readTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * 拉取域的能力清单.
     *
     * @param endpoint 域的初始化服务根地址
     * @return 清单; 拉取失败返回 null (调用方自行决定是否降级)
     */
    public SiteInitManifest fetchManifest(String endpoint) {
        try {
            String body = doGet(endpoint + MANIFEST_PATH);
            return objectMapper.readValue(body, SiteInitManifest.class);
        } catch (Exception e) {
            log.warn("[SiteInit] 拉取能力清单失败: endpoint={} err={}", endpoint, e.getMessage());
            return null;
        }
    }

    /**
     * 调用域执行初始化.
     *
     * @param endpoint 域的初始化服务根地址
     * @param request 调用请求
     * @return 执行结果; 传输层异常时返回 success=false 的结果对象 (不抛)
     */
    public SiteInitInvokeResult invoke(String endpoint, SiteInitInvokeRequest request) {
        try {
            String body = doPost(endpoint + INVOKE_PATH, objectMapper.writeValueAsString(request));
            return objectMapper.readValue(body, SiteInitInvokeResult.class);
        } catch (Exception e) {
            log.warn("[SiteInit] 调用域初始化失败: endpoint={} err={}", endpoint, e.getMessage());
            return SiteInitInvokeResult.fail("调用失败: " + e.getMessage());
        }
    }

    private String doGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Accept", "application/json");
        try {
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code);
            }
            try (InputStream in = conn.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            conn.disconnect();
        }
    }

    private String doPost(String url, String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        try (OutputStream out = conn.getOutputStream()) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        }
        try {
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code);
            }
            try (InputStream in = conn.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            conn.disconnect();
        }
    }
}
