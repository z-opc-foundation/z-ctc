package com.zifang.ctc.common.spi.site;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 建站初始化调用请求 (ctc → 域).
 *
 * <p>标准协议端点 POST {baseUrl}/api/site-init/invoke 接收本对象.
 */
public class SiteInitInvokeRequest {

    /** 站点(租户)编码 */
    private String tenantCode;

    /** 站点(租户)主键 (部分域需要) */
    private Long tenantId;

    /** 要执行的能力编码 (空 = 执行该域全部能力) */
    private String capability;

    /** 调用参数 (与能力 defaultParams 合并后传入) */
    private Map<String, Object> params = new LinkedHashMap<>();

    /** 幂等键: 同一站点 + 同一能力 + 同一幂等键重复调用只生效一次 */
    private String idempotencyKey;

    /** 链路追踪 id */
    private String traceId;

    /** 操作者 (平台管理员账号 id) */
    private String operator;

    public SiteInitInvokeRequest() {
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params == null ? new LinkedHashMap<>() : params;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
