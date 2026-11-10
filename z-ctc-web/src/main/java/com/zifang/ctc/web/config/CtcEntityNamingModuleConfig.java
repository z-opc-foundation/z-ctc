package com.zifang.ctc.web.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 4A 实体入参命名适配 — 与 {@link CtcResponseNamingAdvice} 配对的请求侧一半。
 *
 * <p>背景（2026-11-10「tenantCode 必填」）: all-in-one 宿主的 z-team JacksonConfig 把全局
 * Jackson 策略钉成 SNAKE_CASE ⇒ z-ctc 各 controller 的 {@code @RequestBody} 按 snake_case
 * 反序列化, 前端发的 camelCase 字段 (tenantCode/domainCode/orgName…) 全部绑成 null ——
 * GET 链路有 {@link CtcResponseNamingAdvice} 兜着看着正常, POST 链路却静默丢参。</p>
 *
 * <p>修法: 用 Jackson Module + mixin 给 9 个作为 {@code @RequestBody} 的 DO 实体钉类级
 * {@code @JsonNaming(LowerCamelCaseStrategy)} —— 类级策略优先于宿主全局策略, 且不往
 * z-ctc-core 引 jackson 依赖 (实体本身零注解)。Request DTO (LoginRequest 等) 直接标
 * {@code @JsonNaming}, 不在本 Module 管辖内。</p>
 */
@Configuration
public class CtcEntityNamingModuleConfig {

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    abstract static class CamelNamingMixin {
    }

    @Bean("ctcEntityNamingModule")
    public Module ctcEntityNamingModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "CtcEntityNaming";
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
    }
}
