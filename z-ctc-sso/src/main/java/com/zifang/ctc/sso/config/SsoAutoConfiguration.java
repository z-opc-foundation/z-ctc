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
        String secret = ssoProperties.getJwtSecret();

        // 消费方（sso.ctc-base-url 已配）：token 交回 4A 中心验签（RemoteTokenService），
        // 本地既不签也不验 => 不需要、也不应该持有密钥 => 空值是正常状态。
        // ⚠ 但本 bean 仍必须存在：tokenService(...) 的签名里有它，
        //    所以这里不能因为「消费方用不上」就抛异常把消费方进程打挂。
        if (ssoProperties.isRemoteCtc()) {
            return new JwtUtil(secret == null ? "" : secret);
        }

        // 签发方（4A 中心自己）：本地签发 + 本地验签都要用密钥 => 缺失即启动失败（fail-closed）。
        // ⚠ 绝不静默回落到任何内置默认值：内置默认值会随 jar 一起发布到 Maven 中央，
        //    而中央的已发布坐标不可撤回 => 一旦回落，所有拿到 jar 的人都能离线伪造 sso_token。
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "sso.jwt-secret 未配置：本进程是 4A 中心（sso.ctc-base-url 为空），"
                            + "必须持有 HS256 签名密钥。请以环境变量 SSO_JWT_SECRET 注入"
                            + "（K8s 里请用 secretKeyRef 引用 Secret，不要把密钥写进源码或配置文件）。");
        }
        return new JwtUtil(secret);
    }

}
