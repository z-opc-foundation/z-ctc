package com.zifang.ctc.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.core.ak.model.AkPayload;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 复现 aio 内嵌形态的 AkPayload 绑定链：宿主全局 SNAKE_CASE + CtcEntityNaming mixin
 * ⇒ camelCase body 必须能绑上 ownerType（2026-11-28 aio 联调实测绑 null）。
 */
class CtcEntityNamingModuleConfigAkMixinTest {

    @Test
    void akPayloadCamelBodyBindsUnderSnakeHostMapper() throws Exception {
        ObjectMapper snakeHostMapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .propertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
                .build();
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new MappingJackson2HttpMessageConverter(snakeHostMapper));
        new CtcEntityNamingModuleConfig().extendMessageConverters(converters);

        ObjectMapper serving = ((AbstractJackson2HttpMessageConverter) converters.get(0)).getObjectMapper();
        AkPayload payload = serving.readValue(
                "{\"ownerType\":\"PERSONAL\",\"name\":\"probe\",\"scopesByDomain\":{}}", AkPayload.class);
        assertEquals("PERSONAL", payload.getOwnerType());
        assertEquals("probe", payload.getName());
    }
}
