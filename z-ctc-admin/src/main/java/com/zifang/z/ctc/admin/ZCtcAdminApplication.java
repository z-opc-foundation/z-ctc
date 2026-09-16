package com.zifang.z.ctc.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * z-ctc 4A 中心独立启动类。
 *
 * <p>与 {@code z-meta-admin} / {@code z-mist-admin} 设计一致：
 * <ul>
 *   <li>普通 jar (z-ctc-admin-1.0.0-SNAPSHOT.jar) 作为 library 被 z-opc-main-starter
 *       引用，继续加载 spring.factories 注册的 SsoAutoConfiguration / ZCompanyCtcWebAutoConfiguration。</li>
 *   <li>exec jar (z-ctc-admin-1.0.0-SNAPSHOT-exec.jar) 供独立 Docker 部署使用。</li>
 * </ul>
 *
 * <p>扫描说明：z-ctc 历史包名同时存在 {@code com.zifang.ctc.*}（核心代码）
 * 和 {@code com.zifang.z.ctc.*}（admin 自身），故双包都扫；MyBatis-Plus mapper
 * 集中在 {@code com.zifang.ctc.core.domain.mapper}。
 *
 * <p>FEATURE: 全限定 BeanName 避免 SsoAutoConfiguration (default nameGenerator, short name)
 * 与 ZCompanyCtcWebAutoConfiguration (qualified name) 同时注册同一 @Service 导致
 * NoUniqueBeanDefinitionException.
 */
@SpringBootApplication(scanBasePackages = {
        "com.zifang.ctc",
        "com.zifang.z.ctc"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@ComponentScan(basePackages = {
        "com.zifang.ctc",
        "com.zifang.z.ctc"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class ZCtcAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZCtcAdminApplication.class, args);
    }
}
