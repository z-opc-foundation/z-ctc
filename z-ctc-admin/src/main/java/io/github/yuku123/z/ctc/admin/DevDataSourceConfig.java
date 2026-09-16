package io.github.yuku123.z.ctc.admin;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * z-ctc-admin dev profile 配置：让 z-ctc-web starter 在 dev profile 下用 H2 内存数据库。
 *
 * <p><b>策略</b>：z-ctc-web 的 {@code ZCompanyCtcWebAutoConfiguration#dataSourceCtc} 直接
 * {@code new DruidDataSource()}，不接受 dev 覆盖。dev profile 下我们通过
 * {@code application-dev.yml} 把 {@code z.base.db.ctc.url} 改为 H2 URL，让 starter 自己创建 H2。
 *
 * <p>Tomcat-Druid 实际会通过 {@code z.base.db.ctc.url} / username / password / driver 字段创建连接池，
 * 不会硬编码 MySQL。Driver class 来自 z.base.db.ctc.driver-class-name（默认 mysql），
 * 所以 dev profile 下要显式设 {@code driver-class-name: org.h2.Driver}。
 *
 * <p>本类留空（无 @Bean），仅作占位说明。
 *
 * <p>详见 lead/005_技术架构/005_前端工程与中间件部署架构规范.md §5.3
 */
@Configuration
@Profile("dev")
public class DevDataSourceConfig {
    // 实际逻辑在 application-dev.yml：z.base.db.ctc.url/username/password/driver-class-name
    // 本类仅占位，避免空包。
}