package com.zifang.ctc.common.spi.site;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 建站初始化能力项：某个域能在建站时替站点做的一件事.
 *
 * <p>例: z-cognition 的 "kb.default-library"（建默认知识库）、z-meta 的 "meta.default-dict"（灌默认字典）.
 * 能力项是「清单」的最小单位, ctc 只认 capability.code, 参数走 defaultParams + 调用时覆盖.
 */
public class SiteInitCapability {

    /** 能力编码, 域内唯一 (全平台唯一性由 domainCode + code 组合保证) */
    private String code;

    /** 能力名称 (展示用) */
    private String name;

    /** 能力说明 (展示用) */
    private String description;

    /** 同域内执行顺序, 数字小者先执行 */
    private Integer order;

    /** 默认参数 (建站时由 ctc 合并调用参数覆盖) */
    private Map<String, Object> defaultParams = new LinkedHashMap<>();

    public SiteInitCapability() {
    }

    public SiteInitCapability(String code, String name, String description, Integer order) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.order = order;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Map<String, Object> getDefaultParams() {
        return defaultParams;
    }

    public void setDefaultParams(Map<String, Object> defaultParams) {
        this.defaultParams = defaultParams;
    }
}
