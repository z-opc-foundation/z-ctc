package com.zifang.ctc.sso;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简JWT工具类 - 仅依赖JDK，支持HS256算法
 */
public class JwtUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

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
            // 构建头部
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            String encodedHeader = encodeJson(header);

            // 构建载荷
            Map<String, Object> payload = new HashMap<>(claims);
            payload.put("iat", System.currentTimeMillis() / 1000);
            payload.put("exp", System.currentTimeMillis() / 1000 + expiresIn);
            String encodedPayload = encodeJson(payload);

            // 构建签名
            String data = encodedHeader + "." + encodedPayload;
            String signature = hmacSha256(data, secretKey);

            // 组合JWT
            return data + "." + signature;
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
            // 分割JWT
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return new VerificationResult(false, null, "JWT格式不正确");
            }

            String encodedHeader = parts[0];
            String encodedPayload = parts[1];
            String signature = parts[2];

            // 验证签名
            String data = encodedHeader + "." + encodedPayload;
            String expectedSignature = hmacSha256(data, secretKey);
            if (!signature.equals(expectedSignature)) {
                return new VerificationResult(false, null, "签名验证失败");
            }

            // 解析头部
            Map<String, Object> header = decodeJson(encodedHeader);
            if (!"HS256".equals(header.get("alg"))) {
                return new VerificationResult(false, null, "不支持的算法");
            }

            // 解析载荷
            Map<String, Object> payload = decodeJson(encodedPayload);

            // 验证过期时间
            long exp = getLongClaim(payload, "exp");
            if (exp > 0 && System.currentTimeMillis() / 1000 > exp) {
                return new VerificationResult(false, null, "令牌已过期");
            }

            return new VerificationResult(true, payload, null);
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
     * HMAC-SHA256签名
     */
    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL_ENCODER.encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("签名失败", e);
        }
    }

    /**
     * 编码JSON对象为Base64URL字符串
     */
    private String encodeJson(Map<String, Object> json) {
        String jsonString = toJsonString(json);
        return BASE64_URL_ENCODER.encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解码Base64URL字符串为JSON对象
     */
    private Map<String, Object> decodeJson(String encoded) {
        byte[] bytes = BASE64_URL_DECODER.decode(encoded);
        String jsonString = new String(bytes, StandardCharsets.UTF_8);
        return fromJsonString(jsonString);
    }

    /**
     * 简易JSON转字符串（仅支持Map<String, Object>）
     */
    private String toJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) { sb.append(","); }

            sb.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof List) {
                // 处理List类型，转为JSON数组
                sb.append(value.toString());
            } else {
                sb.append("\"").append(value.toString()).append("\"");
            }

            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 简易字符串转JSON（仅支持简单对象）
     */
    private Map<String, Object> fromJsonString(String json) {
        Map<String, Object> map = new HashMap<>();
        json = json.trim();

        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] keyValues = json.split(",");

            for (String keyValue : keyValues) {
                keyValue = keyValue.trim();
                if (keyValue.isEmpty()) { continue; }


                int colonIndex = keyValue.indexOf(":");
                if (colonIndex > 0) {
                    String key = keyValue.substring(0, colonIndex).trim();
                    String value = keyValue.substring(colonIndex + 1).trim();

                    // 去除引号
                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() - 1);
                    }

                    // 解析值类型
                    Object parsedValue;
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        parsedValue = value.substring(1, value.length() - 1);
                    } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                        parsedValue = Boolean.valueOf(value);
                    } else {
                        try {
                            parsedValue = Long.valueOf(value);
                        } catch (NumberFormatException e) {
                            try {
                                parsedValue = Double.valueOf(value);
                            } catch (NumberFormatException ex) {
                                parsedValue = value;
                            }
                        }
                    }

                    map.put(key, parsedValue);
                }
            }
        }

        return map;
    }

    /**
     * 安全获取long类型声明
     */
    private long getLongClaim(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return -1;
        }
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

    // 示例使用

}    