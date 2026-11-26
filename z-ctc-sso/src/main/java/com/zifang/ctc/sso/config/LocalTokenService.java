package com.zifang.ctc.sso.config;

import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.model.UserInfo;

/**
 * 本地 Token 验证服务 - 直接用 JwtUtil 验证，不走 HTTP 调用
 * 适用于 All-in-One 模式（z-ctc 与各模块在同一 JVM）
 */
public class LocalTokenService implements TokenService {

    private final JwtUtil jwtUtil;

    public LocalTokenService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserInfo verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            JwtUtil.VerificationResult result = jwtUtil.verifyToken(token);
            if (!result.isValid()) {
                return null;
            }
            java.util.Map<String, Object> claims = result.getClaims();
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(String.valueOf(claims.getOrDefault("userId", "")));
            userInfo.setUsername(String.valueOf(claims.getOrDefault("username", "")));
            userInfo.setNickname(String.valueOf(claims.getOrDefault("nickname", "")));
            // FEATURE012 增量: 切租户/域后会重新签发 JWT, 这两个 claim 必须从 claims 复制到 UserInfo,
            // 否则 SsoContext.getCurrentUser().getTenantCode() 永远是 null, 业务方拿不到新上下文.
            // 老 token 没这两个 claim 时, getOrDefault 返回 null, setter 接受 null (String 类型).
            Object tenant = claims.get("tenantCode");
            if (tenant != null) {
                userInfo.setTenantCode(String.valueOf(tenant));
            }
            Object domain = claims.get("domainCode");
            if (domain != null) {
                userInfo.setDomainCode(String.valueOf(domain));
            }
            return userInfo;
        } catch (Exception e) {
            return null;
        }
    }
}
