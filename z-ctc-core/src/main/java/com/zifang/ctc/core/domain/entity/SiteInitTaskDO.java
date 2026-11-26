package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 建站初始化任务: 一次 (站点 × 域 × 能力) 的执行记录.
 */
@BizModel(name = "建站初始化任务", module = "z-ctc", description = "建站初始化任务.")
@TableName("z_ctc_site_init_task")
public class SiteInitTaskDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态: 待执行 */
    public static final int STATUS_PENDING = 0;
    /** 状态: 执行中 */
    public static final int STATUS_RUNNING = 1;
    /** 状态: 成功 */
    public static final int STATUS_SUCCESS = 2;
    /** 状态: 失败 */
    public static final int STATUS_FAILED = 3;
    /** 状态: 跳过 (域未注册该能力 / 被编排策略跳过) */
    public static final int STATUS_SKIPPED = 4;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("tenant_code")
    @BizField(name = "站点(租户)编码", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("domain_code")
    @BizField(name = "域编码", type = BizField.FieldType.STRING)
    private String domainCode;

    @TableField("capability")
    @BizField(name = "能力编码", type = BizField.FieldType.STRING)
    private String capability;

    @TableField("status")
    @BizField(name = "状态: 0=待执行 1=执行中 2=成功 3=失败 4=跳过", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("idempotency_key")
    @BizField(name = "幂等键", type = BizField.FieldType.STRING)
    private String idempotencyKey;

    @TableField("request_json")
    @BizField(name = "请求报文 (JSON)", type = BizField.FieldType.STRING)
    private String requestJson;

    @TableField("response_json")
    @BizField(name = "响应报文 (JSON)", type = BizField.FieldType.STRING)
    private String responseJson;

    @TableField("message")
    @BizField(name = "结果说明 / 失败原因", type = BizField.FieldType.STRING)
    private String message;

    @TableField("cost_ms")
    @BizField(name = "耗时 (毫秒)", type = BizField.FieldType.INTEGER)
    private Long costMs;

    @TableField("trace_id")
    @BizField(name = "链路追踪 id", type = BizField.FieldType.STRING)
    private String traceId;

    @TableField("created_at")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long costMs) {
        this.costMs = costMs;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
