package com.zifang.ctc.common.spi.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 建站初始化调用结果 (域 → ctc).
 *
 * <p>整体 success=false 时 message 给出原因; items 保留逐项明细 (部分成功场景).
 */
public class SiteInitInvokeResult {

    /** 整体是否成功 */
    private boolean success;

    /** 结果码 (成功 0; 失败取业务码) */
    private int code;

    /** 说明 / 失败原因 */
    private String message;

    /** 逐项明细 */
    private List<SiteInitItem> items = new ArrayList<>();

    /** 域侧附带回执数据 */
    private Map<String, Object> data = new LinkedHashMap<>();

    public SiteInitInvokeResult() {
    }

    public static SiteInitInvokeResult ok() {
        SiteInitInvokeResult r = new SiteInitInvokeResult();
        r.success = true;
        r.code = 0;
        return r;
    }

    public static SiteInitInvokeResult fail(String message) {
        SiteInitInvokeResult r = new SiteInitInvokeResult();
        r.success = false;
        r.code = 1;
        r.message = message;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<SiteInitItem> getItems() {
        return items;
    }

    public void setItems(List<SiteInitItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data == null ? new LinkedHashMap<>() : data;
    }
}
