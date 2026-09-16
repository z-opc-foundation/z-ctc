package com.zifang.ctc.web.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.ctc.core.tenant.TenantLineHandlerImpl;
import com.zifang.ctc.web.tenant.OrphanGuardInterceptor;
import com.zifang.z.boot.datasource.starter.ModuleDataSourceTemplate;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

/**
 * z-ctc-web 自动配置 (4A 中心 - 主装配点).
 * <p>
 * 职责：
 * <ul>
 *   <li>注册 ctc 主数据源（{@code dataSourceCtc}） + SqlSessionFactory（{@code sqlSessionFactoryCtc}）</li>
 *   <li>@MapperScan z-ctc-core 全部 mapper 包，复用 sqlSessionFactoryCtc</li>
 *   <li>@ComponentScan z-ctc-web/api + z-ctc-core/service/domain + z-ctc-common 下的 Service / Controller / Properties</li>
 *   <li>注册 MyBatis-Plus 分页拦截器（{@link PaginationInnerInterceptor}），否则 {@code BaseMapper.selectPage()} 会抛 {@code BadSqlGrammarException}</li>
 * </ul>
 * <p>
 * 使用 {@link FullyQualifiedAnnotationBeanNameGenerator} 避免同名 Bean 冲突。
 */
@Configuration
@MapperScan(
        basePackages = "com.zifang.ctc.core.domain.mapper",
        sqlSessionFactoryRef = "sqlSessionFactoryCtc",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@ComponentScan(
        value = {
                "com.zifang.ctc.web.api",
                "com.zifang.ctc.web.service",  // 验证码服务 (注册/验证码登录/找回密码)
                "com.zifang.ctc.web.tenant",  // FEATURE049: 游离态守卫拦截器
                "com.zifang.ctc.web.init",    // FEATURE050: z-ctc 内置 TenantInitializer
                "com.zifang.ctc.core.service",
                "com.zifang.ctc.core.domain",
                "com.zifang.ctc.core.init",   // FEATURE050: 租户初始化器 (TenantInitOrchestrator + 各模块 Initializer)
                "com.zifang.ctc.common"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class ZCompanyCtcWebAutoConfiguration extends ModuleDataSourceTemplate implements WebMvcConfigurer {

    /**
     * FEATURE049: 游离态守卫拦截器 (Bean 而非依赖 @ComponentScan)
     */
    @Bean("orphanGuardInterceptor")
    public OrphanGuardInterceptor orphanGuardInterceptor() {
        return new OrphanGuardInterceptor();
    }

    /**
     * FEATURE049: 注册游离态守卫拦截器.
     * <p>
     * 在所有 /api/** 请求前检查游离态, 拒绝访问非白名单接口.
     * SsoInterceptor 已经在 Spring MVC 拦截器链中, 本拦截器在 Sso 之后.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(orphanGuardInterceptor())
                .addPathPatterns("/api/**")
                .order(0);  // 高优先级, 在业务 controller 之前
    }

    @Bean("dataSourceCtc")
    @ConditionalOnMissingBean(name = "dataSourceCtc")
    public DataSource dataSourceCtc(org.springframework.core.env.Environment env) {
        // FEATURE: 支持 z.base.db.ctc.url 直接覆盖(避免阿里云 RDS Public Key Retrieval 问题)
        String overrideUrl = env.getProperty("z.base.db.ctc.url");
        if (overrideUrl != null && !overrideUrl.isEmpty()) {
            com.alibaba.druid.pool.DruidDataSource ds = new com.alibaba.druid.pool.DruidDataSource();
            ds.setUrl(overrideUrl);
            ds.setUsername(env.getProperty("z.base.db.ctc.username", "root"));
            ds.setPassword(env.getProperty("z.base.db.ctc.password", ""));
            int initialSize = env.getProperty("z.base.db.ctc.initial-size", Integer.class, 5);
            int minIdle = env.getProperty("z.base.db.ctc.min-idle", Integer.class, 5);
            int maxActive = env.getProperty("z.base.db.ctc.max-active", Integer.class, 20);
            int maxWait = env.getProperty("z.base.db.ctc.max-wait", Integer.class, 60000);
            ds.setInitialSize(initialSize);
            ds.setMinIdle(minIdle);
            ds.setMaxActive(maxActive);
            ds.setMaxWait(maxWait);
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            return ds;
        }
        return buildDataSource(env, "ctc");
    }

    /**
     * MyBatis-Plus 拦截器 — 注册分页拦截器。
     * <p>
     * 没有 {@link PaginationInnerInterceptor} 时，{@code BaseMapper.selectPage(IPage, Wrapper)}
     * 不会自动拼接 {@code LIMIT ...} 子句，会触发 {@code BadSqlGrammarException}。
     * 历史原因：{@code ModuleDataSourceTemplate#buildSqlSessionFactory} 只设了 mapper/typeAliases，
     * 没设 plugins；Controller 调 {@code pageResourcesByApp(...)} 触发 {@code selectPage} 时就炸。
     */
    @Bean("mybatisPlusInterceptorCtc")
    @ConditionalOnMissingBean(name = "mybatisPlusInterceptorCtc")
    public MybatisPlusInterceptor mybatisPlusInterceptorCtc() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // FEATURE049: 租户隔离拦截器 — 自动追加 WHERE tenant_code = ? / INSERT 注入 tenant_code
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandlerImpl()));
        return interceptor;
    }

    @Bean("sqlSessionFactoryCtc")
    @ConditionalOnMissingBean(name = "sqlSessionFactoryCtc")
    public MybatisSqlSessionFactoryBean sqlSessionFactoryCtc(
            DataSource dataSourceCtc,
            MybatisPlusInterceptor mybatisPlusInterceptorCtc) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSourceCtc);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:/mapper/**/*.xml"));
        factoryBean.setTypeAliasesPackage("com.zifang.ctc.core.domain.entity");
        factoryBean.setPlugins(new Interceptor[]{mybatisPlusInterceptorCtc});
        return factoryBean;
    }
}
