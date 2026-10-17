package com.zifang.ctc.client;

import com.zifang.ctc.client.model.ZCtcResource;
import com.zifang.ctc.client.model.ZCtcUserGrant;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 远端 HTTP 实现（同 bench-client / msg-client / evolution-client 风格：HttpURLConnection，零额外 HTTP 依赖）。
 *
 * <p>4A 中心所有端点都回 {@code Result{success, data, code, message}} 信封，
 * 这里统一取 {@code data}；{@code success=false} 或 HTTP 非 2xx 一律抛
 * {@link ZCtcClientException}，让消费方的错误处理只有一个形状。
 */
public class RemoteZCtcClient implements ZCtcClient {

    private final String baseUrl;
    private final int connectTimeoutMs = 5000;
    private final int readTimeoutMs = 30000;

    public RemoteZCtcClient(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "";
    }

    @Override
    public List<ZCtcResource> listAllResources(String token) {
        return list("/api/ctc/authz/resources/list", token);
    }

    @Override
    public List<ZCtcResource> listUserPermissions(String token, Long userId) {
        return list("/api/ctc/authz/users/permissions?userId=" + userId, token);
    }

    @Override
    public List<ZCtcUserGrant> listUserGrants(String token, Long userId) {
        Object data = call("GET", "/api/ctc/authz/users/grants?userId=" + userId, token, null, null);
        if (data == null) {
            return Collections.emptyList();
        }
        List<ZCtcUserGrant> list = JsonUtil.fromJson(JsonUtil.toJson(data),
                new TypeReference<List<ZCtcUserGrant>>() {});
        return list == null ? Collections.<ZCtcUserGrant>emptyList() : list;
    }

    @Override
    public void grantUserResource(String token, Long userId, Long resourceId, String grantedBy) {
        Map<String, String> headers = grantedBy == null || grantedBy.trim().isEmpty()
                ? null
                : Collections.singletonMap("X-User-Id", grantedBy.trim());
        call("POST", "/api/ctc/authz/users/grant-resource?userId=" + userId + "&resourceId=" + resourceId,
                token, headers, null);
    }

    @Override
    public void revokeUserResource(String token, Long userId, Long resourceId) {
        call("DELETE", "/api/ctc/authz/users/revoke-resource?userId=" + userId + "&resourceId=" + resourceId,
                token, null, null);
    }

    // ========== HTTP 工具 ==========

    private List<ZCtcResource> list(String path, String token) {
        Object data = call("GET", path, token, null, null);
        if (data == null) {
            return Collections.emptyList();
        }
        List<ZCtcResource> list = JsonUtil.fromJson(JsonUtil.toJson(data),
                new TypeReference<List<ZCtcResource>>() {});
        return list == null ? Collections.<ZCtcResource>emptyList() : list;
    }

    private Object call(String method, String path, String token, Map<String, String> headers,
                        String body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token.trim());
            }
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    conn.setRequestProperty(e.getKey(), e.getValue());
                }
            }
            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }
            }

            int code = conn.getResponseCode();
            // authz 域一律非 2xx 即抛：401 在这里是「调用方 token 没传对」，
            // 静默返空列表会表现成「该用户一个权限都没有」，比报错难查得多。
            if (code < 200 || code >= 300) {
                String detail = read(conn.getErrorStream());
                throw new ZCtcClientException("z-ctc " + method + " " + path + " 失败: HTTP " + code
                        + (detail == null || detail.trim().isEmpty() ? "" : " — " + trim(detail)));
            }
            return unwrapData(read(conn.getInputStream()), method + " " + path);
        } catch (ZCtcClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ZCtcClientException("z-ctc " + method + " " + path + " 调用异常", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 拆 {@code Result{success,data,code,message}} 信封取 data。
     * data 是嵌套结构，必须先 toPlainValue 摊平 —— 解析器直接吐的
     * {@code JsonObject} 强转 POJO 会丢内容（历史上踩过：转成 String 再
     * fromJson 是唯一稳的写法）。
     */
    private Object unwrapData(String json, String where) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        Map<String, Object> envelope = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {});
        if (envelope == null) {
            return null;
        }
        if (Boolean.FALSE.equals(envelope.get("success"))) {
            Object message = envelope.get("message");
            throw new ZCtcClientException("z-ctc " + where + " 返回失败: "
                    + (message != null ? String.valueOf(message) : "无 message"));
        }
        return JsonUtil.toPlainValue(envelope.get("data"));
    }

    private String read(InputStream is) throws Exception {
        if (is == null) {
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        } finally {
            reader.close();
        }
    }

    private static String trim(String s) {
        return s.length() > 500 ? s.substring(0, 500) + "…" : s;
    }
}
