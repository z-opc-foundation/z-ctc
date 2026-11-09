package com.zifang.ctc.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * z-ctc 响应字段命名固定为 lowerCamelCase, 不随宿主的全局 Jackson 命名策略漂移.
 * <p>
 * 背景: 独立部署时 z-ctc 走 Boot 默认 camelCase 契约 (前端组件与外部调用方都按它写).
 * 2026-11-07 z-ctc-web 内嵌进 all-in-one 主壳后, z-team-core 的 JacksonConfig 把全局
 * ObjectMapper 设成 SNAKE_CASE ⇒ /api/ctc/** 响应静默变成 tenant_name / tenant_code,
 * 4A 前端按 tenantName / tenantCode 读 ⇒ 字段全空 (租户树只剩状态角标), 外部调用方同样会断.
 * <p>
 * 做法: 只对 ctc 包内控制器 (basePackages=com.zifang.ctc), 用宿主 ObjectMapper 的拷贝
 * 把响应体先序列化成树 (字段名此时已按 camelCase 定死), 再交给原转换器写出 ——
 * 宿主的 snake_case 命名策略在写出树时不再参与. 独立部署时 camelCase → camelCase 为恒等变换.
 */
@ControllerAdvice(basePackages = "com.zifang.ctc")
public class CtcResponseNamingAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper camelCaseMapper;

    public CtcResponseNamingAdvice(ObjectMapper objectMapper) {
        this.camelCaseMapper = objectMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || selectedContentType == null
                || !MediaType.APPLICATION_JSON.includes(selectedContentType)) {
            return body;
        }
        return camelCaseMapper.valueToTree(body);
    }
}
