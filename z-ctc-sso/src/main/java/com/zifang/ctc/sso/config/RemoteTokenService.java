package com.zifang.ctc.sso.config;

import com.zifang.ctc.sso.cache.TokenCache;
import com.zifang.ctc.sso.model.UserInfo;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * 远端 Token 验签 —— 本进程是<b>消费方</b>时用：把 token 交回 4A 中心判，本地不验签。
 *
 * <p>与 {@link LocalTokenService} 的分工由 {@link SsoProperties#isRemoteCtc()} 决定：
 * 配了 {@code sso.ctc-base-url} 走这里，留空走本地验签。
 *
 * <p><b>故障域</b>：这条链路只打 4A 中心的 {@code /api/ctc/authn/verify}，
 * 那个端点在服务端只做 {@code JwtUtil.verifyToken}，<b>不查数据库</b>。
 * 因此它与「读权限点」那条链路（z-ctc-client，要连 {@code ctc_resource} 表）
 * 必须分开——否则 4A 中心 DB 抖一下会同时打掉登录态与权限。
 *
 * <p><b>修的是什么</b>：旧实现打的是 {@code sso.auth-server-url}（默认值
 * {@code http://localhost:8080/auth/verify}）——那个路径 4A 中心并不存在，
 * 它注册在 {@code /api/ctc/authn/verify}；而且把 {@code Result} 信封当裸
 * {@code UserInfo} 解。两处独立的错叠加，结果是「配了远端就永远认不出登录用户」，
 * 且不报错。该实现从未被装配过。
 */
public class RemoteTokenService implements TokenService {

    private static final Logger log = LogManager.getLogger(RemoteTokenService.class);

    private final String verifyUrl;
    private final TokenCache cache;

    public RemoteTokenService(SsoProperties ssoProperties) {
        this(ssoProperties.getCtcBaseUrl(), new TokenCache());
    }

    public RemoteTokenService(String verifyUrl, TokenCache cache) {
        String base = verifyUrl == null ? "" : verifyUrl.trim().replaceAll("/+$", "");
        this.verifyUrl = base + "/api/ctc/authn/verify";
        this.cache = cache;
    }

    @Override
    public UserInfo verifyToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        token = token.trim();

        UserInfo cached = cache.get(token);
        if (cached != null) {
            return cached;
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(verifyUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                // 4A 中心不可达：按未登录处理，但必须留痕。静默返 null 会让
                // 「全站权限为空」这种症状在现场无从追因。
                log.warn("[SSO] 4A 中心验签失败 HTTP {}，按未登录处理: {}", code, verifyUrl);
                return null;
            }

            // 信封是 Result{success,data:{claims,valid},...}，claims 才是 token 里解出来的身份。
            Map<String, Object> envelope = JsonUtil.fromJson(read(conn.getInputStream()),
                    new TypeReference<Map<String, Object>>() {});
            if (envelope == null || Boolean.FALSE.equals(envelope.get("success"))) {
                return null;
            }
            Object data = JsonUtil.toPlainValue(envelope.get("data"));
            if (!(data instanceof Map)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) data;
            if (!Boolean.TRUE.equals(body.get("valid"))) {
                return null;
            }
            Object claimsObj = JsonUtil.toPlainValue(body.get("claims"));
            if (!(claimsObj instanceof Map)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = (Map<String, Object>) claimsObj;

            UserInfo info = new UserInfo();
            info.setUserId(str(claims.get("userId")));
            info.setUsername(str(claims.get("username")));
            info.setNickname(str(claims.get("nickname")));
            info.setTenantCode(str(claims.get("tenantCode")));
            info.setDomainCode(str(claims.get("domainCode")));
            info.setRoles(Collections.<String>emptySet());
            info.setPermissions(Collections.<String>emptySet());

            cache.put(token, info);
            return info;
        } catch (Exception e) {
            log.warn("[SSO] 4A 中心验签异常，按未登录处理: {}", e.toString());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String read(InputStream is) throws Exception {
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
}
