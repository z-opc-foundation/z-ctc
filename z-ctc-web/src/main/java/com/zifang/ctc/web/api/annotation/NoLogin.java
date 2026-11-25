package com.zifang.ctc.web.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 免登录标记: SSO 拦截器按注解**简单名**识别 (见 z-ctc-sso 的 SsoInterceptor),
 * 故本仓自定义同名注解即可让端点跳过登录态校验.
 *
 * <p>仅用于服务间机器调用 (如各域向 4A 中心登记建站初始化能力), 这类端点必须另配
 * 自己的鉴权 (如 X-Site-Init-Token), 不能裸奔.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface NoLogin {
}
