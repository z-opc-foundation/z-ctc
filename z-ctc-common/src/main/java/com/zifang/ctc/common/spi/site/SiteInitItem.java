package com.zifang.ctc.common.spi.site;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单项初始化结果 (域 → ctc): 一个能力项的执行明细.
 */
public class SiteInitItem {

    /** 能力编码 */
    private String code;

    /** 能力名称 */
    private String name;

    /** 是否成功 */
    private boolean success;

    /** 失败原因 / 说明 */
    private String message;

    /** 该项产出的资源描述 (如建出来的知识库 id, 域编码) */
    private Map<String, Object> data = new LinkedHashMap<>();

    public SiteInitItem() {
    }

    public SiteInitItem(String code, String name, boolean success, String message) {
        this.code = code;
        this.name = name;
        this.success = success;
        this.message = message;
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data == null ? new LinkedHashMap<>() : data;
    }
}
