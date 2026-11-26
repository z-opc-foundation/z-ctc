package com.zifang.ctc.sso;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JwtUtilTest {

    public static void main(String[] args) {
        JwtUtil jwtUtil = new JwtUtil("yourSecretKey");

        // 创建声明
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "12345");
        claims.put("username", "john.doe");
        claims.put("roles", Arrays.asList("user", "admin"));

        // 生成令牌（有效期3600秒）
        String token = jwtUtil.generateToken(claims, 3600);
        System.out.println("生成的JWT: " + token);

        // 验证令牌
        JwtUtil.VerificationResult result = jwtUtil.verifyToken(token);
        if (result.isValid()) {
            System.out.println("验证成功");
            System.out.println("声明内容: " + result.getClaims());
        } else {
            System.out.println("验证失败: " + result.getErrorMessage());
        }
    }


}
