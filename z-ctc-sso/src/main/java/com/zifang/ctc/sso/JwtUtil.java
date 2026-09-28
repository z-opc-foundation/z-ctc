package com.zifang.ctc.sso;

import com.zifang.util.core.jwt.Claims;
import com.zifang.util.core.jwt.Jwt;
import com.zifang.util.core.jwt.JwtException;

import java.util.HashMap;
import java.util.Map;

/**
 * 极简 JWT 工具类 —— HS256 三段式 token。
 * <p>
 * 2026-09-28 收编：签名(HmacSHA256)、base64url(无填充)、claims 的 JSON 序列化/解析、
 * exp 校验全部委托 {@code com.zifang.util.core.jwt.Jwt}（z-util 1.0.13），
 * 本类只保留历史对外形状：实例持密钥 + {@code generateToken(Map, long)} +
 * {@code verifyToken(String) -> VerificationResult}（不抛异常，用 valid 位表达失败）。
 * <p>
 * 兼容性结论（对拍尺见 {@code src/test/java/com/zifang/ctc/sso/JwtCompatCrossCheckTest}）：
 * <ul>
 *   <li>签名原文 = {@code headerB64 + "." + payloadB64} 的字面 UTF-8 字节，算法 HmacSHA256、
 *       密钥取 secret 的 UTF-8 字节 —— 与收编前完全一致，故历史 token 照常验签；</li>
 *   <li>payload 段逐字节相同：{@link #generateToken} 刻意先把入参过一遍 {@code HashMap}，
 *       复刻收编前 {@code new HashMap<>(claims)} 的迭代顺序（勿改成 LinkedHashMap/TreeMap）；</li>
 *   <li>header 段仅键序不同：收编前 HashMap 迭代出 {@code {"typ":"JWT","alg":"HS256"}}，
 *       z-util 固定 {@code {"alg":"HS256","typ":"JWT"}}。验签读的是字面段，不影响互验；</li>
 *   <li>过期语义：有 exp 且 {@code now > exp} 判过期，与收编前一致；
 *       无 exp 时不判过期（未开 requireExp），亦与收编前一致。</li>
 * </ul>
 * 已知收紧（不放宽校验）：{@code exp <= 0} 的 token 现在按过期处理（收编前 {@code exp > 0} 才判）；
 * nbf 若存在会被校验（签发链路从不写 nbf）；payload 非法 JSON（如历史手写的 List claim
 * 被 {@code toString()} 成 {@code [a, b]}）现在判无效——签发链路只写 String/Integer/Boolean，不产生该类 token。
 */
public class JwtUtil {

    private final String secretKey;

    public JwtUtil(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * 生成JWT令牌
     *
     * @param claims    声明信息
     * @param expiresIn 过期时间（秒）
     * @return JWT令牌
     */
    public String generateToken(Map<String, Object> claims, long expiresIn) {
        try {
            // 见类注释：走 HashMap 迭代顺序，保证 payload 段与收编前逐字节一致
            Map<String, Object> payload = new HashMap<String, Object>(claims);
            long now = System.currentTimeMillis() / 1000;
            payload.put("iat", now);
            payload.put("exp", now + expiresIn);

            Claims claimsObj = new Claims();
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                claimsObj.put(entry.getKey(), entry.getValue());
            }
            return Jwt.builder().algorithm(Jwt.HS256).secret(secretKey).claims(claimsObj).build();
        } catch (Exception e) {
            throw new RuntimeException("生成JWT失败", e);
        }
    }

    /**
     * 验证JWT令牌
     *
     * @param token JWT令牌
     * @return 验证结果，包含是否有效和声明信息
     */
    public VerificationResult verifyToken(String token) {
        try {
            Claims parsed = Jwt.parser().algorithm(Jwt.HS256).secret(secretKey).parse(token);
            return new VerificationResult(true, toLegacyClaims(parsed), null);
        } catch (JwtException e) {
            return new VerificationResult(false, null, describe(e.getMessage()));
        } catch (Exception e) {
            return new VerificationResult(false, null, "验证过程发生异常: " + e.getMessage());
        }
    }

    /**
     * 从JWT中提取声明信息
     *
     * @param token JWT令牌
     * @return 声明信息
     */
    public Map<String, Object> getClaims(String token) {
        VerificationResult result = verifyToken(token);
        return result.isValid() ? result.getClaims() : null;
    }

    /**
     * z-util 的失败文案映射回收编前的中文提示（前端 401 文案与日志 grep 口径不变）。
     */
    private static String describe(String zUtilMessage) {
        String msg = zUtilMessage == null ? "" : zUtilMessage;
        if (msg.startsWith("malformed JWT")) {
            return "JWT格式不正确";
        }
        if (msg.startsWith("signature mismatch")) {
            return "签名验证失败";
        }
        if (msg.startsWith("alg mismatch")) {
            return "不支持的算法";
        }
        if (msg.startsWith("token expired")) {
            return "令牌已过期";
        }
        if (msg.startsWith("token not yet valid")) {
            return "令牌尚未生效";
        }
        return "验证过程发生异常: " + msg;
    }

    /**
     * claims 回读形状对齐收编前：整数统一 Long（收编前的暴力解析不产 Integer）。
     */
    private static Map<String, Object> toLegacyClaims(Claims parsed) {
        Map<String, Object> claims = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : parsed.asMap().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
                value = Long.valueOf(((Number) value).longValue());
            }
            claims.put(entry.getKey(), value);
        }
        return claims;
    }

    /**
     * 验证结果类
     */
    public static class VerificationResult {
        private final boolean valid;
        private final Map<String, Object> claims;
        private final String errorMessage;

        public VerificationResult(boolean valid, Map<String, Object> claims, String errorMessage) {
            this.valid = valid;
            this.claims = claims;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public Map<String, Object> getClaims() {
            return claims;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

}
