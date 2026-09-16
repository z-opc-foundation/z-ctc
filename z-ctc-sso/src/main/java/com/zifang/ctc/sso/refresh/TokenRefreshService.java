package com.zifang.ctc.sso.refresh;

import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.model.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.zifang.util.core.lang.RandomUtil;

/**
 * Refresh Token 服务.
 * <p>
 * 设计哲学:
 * 短期 access token + 长期 refresh token 是当前主流 SSO 协议 (OAuth 2.0 / OIDC) 的标准模式.
 * 本类与既有 {@link com.zifang.ctc.sso.config.LocalTokenService} / {@link com.zifang.ctc.sso.config.RemoteTokenService}
 * 完全解耦 — 不修改它们的接口, 仅作为"refresh token 颁发 / 续期"的能力扩展.
 * <p>
 * 业务流程 (业务方参考):
 * <ol>
 *   <li>用户登录成功 → 业务方调用 {@link #issueRefreshToken(UserInfo)} 颁发 refresh token, 同时业务方自行颁发 access token</li>
 *   <li>access token 过期 → 业务方调用 {@link #refresh(String)} 拿新 access token</li>
 *   <li>refresh token 也过期 / 用户主动登出 → 业务方调用 {@link #revoke(String)} 销毁</li>
 * </ol>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>refresh token 复用既有 {@link JwtUtil} 签名 (与 access token 同源, 业务方可区分类型字段)</li>
 *   <li>refresh token 30 天 TTL, access token 由业务方决定 (默认 JwtUtil 不带过期, 业务方写 token 时自管)</li>
 *   <li>in-memory revoked 列表 (与 MultiTerminalSessionStore 风格一致)</li>
 *   <li>不修改任何现有类: 本类纯新增, 不动 TokenService / SsoProperties / SsoInterceptor / JwtUtil</li>
 * </ul>
 */
@Service
public class TokenRefreshService {

    public static final long REFRESH_TOKEN_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000;
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_REFRESH_ID = "refreshId";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final Logger log = LogManager.getLogger(TokenRefreshService.class);
    private final java.util.Set<String> revokedRefreshIds =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 为用户颁发 refresh token. 内部用 JwtUtil 签发 JWT, claims 含 tokenType=refresh + refreshId (UUID).
     *
     * @return refresh token 字符串 (业务方持久化给前端, 不入 z-ctc 数据库)
     */
    public String issueRefreshToken(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }
        String refreshId = RandomUtil.uuid();
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        claims.put(CLAIM_REFRESH_ID, refreshId);
        claims.put("userId", userInfo.getUserId());
        claims.put("username", userInfo.getUsername());
        claims.put("nickname", userInfo.getNickname());
        return jwtUtil.generateToken(claims, REFRESH_TOKEN_TTL_MILLIS / 1000);
    }

    /**
     * 拿 refresh token 换新 access token. 业务方根据业务需要自行决定 access token 的 TTL/载荷.
     * <p>
     * 本方法返回验证结果 + 解析出的 claims, 由业务方决定如何签发 access token.
     *
     * @return RefreshResult, 业务方根据 valid 判断是否换发
     */
    public RefreshResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return RefreshResult.invalid();
        }
        try {
            JwtUtil.VerificationResult v = jwtUtil.verifyToken(refreshToken);
            if (!v.isValid()) {
                return RefreshResult.invalid();
            }
            Object tokenType = v.getClaims().get(CLAIM_TOKEN_TYPE);
            if (!TOKEN_TYPE_REFRESH.equals(tokenType)) {
                log.debug("refresh token 校验失败: tokenType 不是 refresh, 实际={}", tokenType);
                return RefreshResult.invalid();
            }
            Object refreshIdObj = v.getClaims().get(CLAIM_REFRESH_ID);
            if (refreshIdObj == null) {
                return RefreshResult.invalid();
            }
            String refreshId = String.valueOf(refreshIdObj);
            if (revokedRefreshIds.contains(refreshId)) {
                log.debug("refresh token 已被吊销: refreshId={}", refreshId);
                return RefreshResult.invalid();
            }

            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(String.valueOf(v.getClaims().getOrDefault("userId", "")));
            userInfo.setUsername(String.valueOf(v.getClaims().getOrDefault("username", "")));
            userInfo.setNickname(String.valueOf(v.getClaims().getOrDefault("nickname", "")));
            return RefreshResult.valid(userInfo, refreshId);
        } catch (Exception e) {
            log.warn("refresh token 校验异常: {}", e.getMessage());
            return RefreshResult.invalid();
        }
    }

    /**
     * 吊销某个 refresh token. 业务方在用户登出 / refresh 轮换时调用.
     */
    public void revoke(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        try {
            JwtUtil.VerificationResult v = jwtUtil.verifyToken(refreshToken);
            if (!v.isValid()) {
                return;
            }
            Object refreshIdObj = v.getClaims().get(CLAIM_REFRESH_ID);
            if (refreshIdObj != null) {
                revokedRefreshIds.add(String.valueOf(refreshIdObj));
            }
        } catch (Exception e) {
            // ignore: 吊销时 token 异常不阻断业务
        }
    }

    /**
     * 清空吊销列表 (单元测试 / 配置热加载).
     */
    public void clearRevoked() {
        revokedRefreshIds.clear();
    }

    /**
     * refresh token 校验结果.
     */
    public static class RefreshResult {
        private final boolean valid;
        private final UserInfo userInfo;
        private final String refreshId;

        private RefreshResult(boolean valid, UserInfo userInfo, String refreshId) {
            this.valid = valid;
            this.userInfo = userInfo;
            this.refreshId = refreshId;
        }

        public static RefreshResult valid(UserInfo userInfo, String refreshId) {
            return new RefreshResult(true, userInfo, refreshId);
        }

        public static RefreshResult invalid() {
            return new RefreshResult(false, null, null);
        }

        public boolean isValid() {
            return valid;
        }

        public UserInfo getUserInfo() {
            return userInfo;
        }

        public String getRefreshId() {
            return refreshId;
        }
    }
}
