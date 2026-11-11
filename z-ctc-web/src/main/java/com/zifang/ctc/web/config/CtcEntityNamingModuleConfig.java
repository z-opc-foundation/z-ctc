package com.zifang.ctc.web.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.DeptDO;
import com.zifang.ctc.core.domain.entity.DomainDO;
import com.zifang.ctc.core.domain.entity.GroupDO;
import com.zifang.ctc.core.domain.entity.MenuDO;
import com.zifang.ctc.core.domain.entity.OrgDO;
import com.zifang.ctc.core.domain.entity.ResourceDO;
import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.domain.entity.TenantDO;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 4A 实体入参命名适配 — 与 {@link CtcResponseNamingAdvice} 配对的请求侧一半。
 *
 * <p>背景（2026-11-10「tenantCode 必填」）: all-in-one 宿主的 z-team JacksonConfig 把全局
 * Jackson 策略钉成 SNAKE_CASE ⇒ z-ctc 各 controller 的 {@code @RequestBody} 按 snake_case
 * 反序列化, 前端发的 camelCase 字段 (tenantCode/domainCode/orgName…) 全部绑成 null ——
 * GET 链路有 {@link CtcResponseNamingAdvice} 兜着看着正常, POST 链路却静默丢参。</p>
 *
 * <p>修法: mixin 给 9 个作为 {@code @RequestBody} 的 DO 实体钉类级
 * {@code @JsonNaming(LowerCamelCaseStrategy)} —— 类级策略优先于宿主全局策略, 且不往
 * z-ctc-core 引 jackson 依赖 (实体本身零注解)。Request DTO (LoginRequest 等) 直接标
 * {@code @JsonNaming}, 不在本类管辖内。</p>
 *
 * <p><b>注册通道必须走 {@code extendMessageConverters}, 不能用 {@code @Bean Module}</b>
 * (2026-11-11 实测): 宿主链上存在调用 {@code modulesToInstall(...)} 的 customizer
 * (z-boot-jackson-starter GlobalJacksonConfig), 它整体替换 module 列表, 把 Boot 汇总的
 * {@code @Bean Module} 无声清掉 —— z-team JacksonConfig 注释里对 ZUtilJsonModel 记过同一坑。
 * 直接往 MVC 转换器的 mapper 上 {@code registerModule} 不经过 builder, 谁也抢不走。
 * 独立部署 (z-ctc-admin 扫 com.zifang.ctc) 与内嵌 all-in-one (ZCompanyCtcWebAutoConfiguration
 * {@code @Import} 本类) 两种形态 MVC 转换器初始化都会经过这里。</p>
 */
@Configuration
public class CtcEntityNamingModuleConfig implements WebMvcConfigurer {

    static final String MODULE_ID = "CtcEntityNaming";

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    abstract static class CamelNamingMixin {
    }

    private static final Module CTC_ENTITY_NAMING_MODULE = new Module() {
        @Override
        public String getModuleName() {
            return MODULE_ID;
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public void setupModule(SetupContext context) {
            Class<?>[] entities = {
                    AccountDO.class, DeptDO.class, DomainDO.class, GroupDO.class,
                    MenuDO.class, OrgDO.class, ResourceDO.class, RoleDO.class, TenantDO.class,
            };
            for (Class<?> entity : entities) {
                context.setMixInAnnotations(entity, CamelNamingMixin.class);
            }
        }
    };

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (!(converter instanceof AbstractJackson2HttpMessageConverter)) {
                continue;
            }
            ObjectMapper mapper = ((AbstractJackson2HttpMessageConverter) converter).getObjectMapper();
            if (mapper.getRegisteredModuleIds().contains(MODULE_ID)) {
                continue;
            }
            mapper.registerModule(CTC_ENTITY_NAMING_MODULE);
        }
    }
}
