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

    /**
     * 延迟取 TokenService：不能存成实例字段（@Bean 方法何时跑不由 addInterceptors 决定），
     * 也不能 {@code @Autowired TokenService} 自注入本类定义的 bean（自引用环）。
     */
    private final org.springframework.beans.factory.ObjectProvider<TokenService> tokenServiceProvider;

    public SsoAutoConfiguration(org.springframework.beans.factory.ObjectProvider<TokenService> tokenServiceProvider) {
        this.tokenServiceProvider = tokenServiceProvider;
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenService tokenService(SsoProperties ssoProperties, JwtUtil jwtUtil) {
        // sso.ctc-base-url 配了 = 本进程是消费方，登录态交回 4A 中心判；
        // 留空 = 本进程自己就是 4A 中心（all-in-one），本地验签。
        TokenService svc = ssoProperties.isRemoteCtc()
                ? new RemoteTokenService(ssoProperties)
                : new LocalTokenService(jwtUtil);
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
        // 必须在这里现取：addInterceptors 由 WebMvcConfigurerComposite 在 MVC 自动配置
        // 装配期调用，那一刻 tokenService() 这个 @Bean 方法通常还没跑过，
        // 早先把实例存进字段只会得到 null（SsoInterceptor.preHandle 第 74 行 NPE → 500）。
        // getObject() 顺带强制 bean 真正创建一次，RemoteTokenService 的 TokenCache 不会被劈成两半。
        SsoInterceptor interceptor = new SsoInterceptor(tokenServiceProvider.getObject(), ssoProperties);
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
