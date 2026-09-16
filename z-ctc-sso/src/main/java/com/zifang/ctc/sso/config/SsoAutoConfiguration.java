package com.zifang.ctc.sso.config;


import com.zifang.ctc.sso.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 */
@Configuration
@EnableConfigurationProperties(SsoProperties.class)
@ComponentScan("com.zifang.ctc.sso")
public class SsoAutoConfiguration implements WebMvcConfigurer {

    private static final Logger log = LogManager.getLogger(SsoAutoConfiguration.class);

    @Autowired
    private SsoProperties ssoProperties;

    @Bean
    @ConditionalOnMissingBean
    public TokenService tokenService(SsoProperties ssoProperties, JwtUtil jwtUtil) {
        TokenService svc = new LocalTokenService(jwtUtil);
        // FEATURE057: 注入到 SsoContext 静态字段, 让 controller 端 (e.g. TeamCurrentUserResolver)
        // 能直接调用 SsoContext.verifyTokenFromCookie(token) 验证 cookie 中的 JWT.
        SsoContext.init(svc);
        return svc;
    }

    @Bean
    public SsoInterceptor ssoInterceptor(TokenService tokenService, SsoProperties ssoProperties) {
        return new SsoInterceptor(tokenService, ssoProperties);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SsoInterceptor interceptor = new SsoInterceptor(
                tokenService(ssoProperties, jwtUtil()),
                ssoProperties
        );
        // 读取 application.properties 中的 sso.exclude-paths 配置（不再硬编码）
        interceptor.setExcludedPaths(ssoProperties.getExcludePaths());
        interceptor.setInterceptPaths(ssoProperties.getInterceptPaths());
        // 域名 → 租户/域 覆盖规则 (YAML: sso.domain-overrides)
        if (ssoProperties.getDomainOverrides() != null && !ssoProperties.getDomainOverrides().isEmpty()) {
            interceptor.setDomainOverrides(ssoProperties.getDomainOverrides());
            System.out.println("=== SsoAutoConfiguration: loaded " + ssoProperties.getDomainOverrides().size() + " domain overrides");
        }
        registry.addInterceptor(interceptor).addPathPatterns("/**");
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtUtil jwtUtil() {
        return new JwtUtil("ctc-secret-key-2024-secure-jwt-signing-key");
    }

}
