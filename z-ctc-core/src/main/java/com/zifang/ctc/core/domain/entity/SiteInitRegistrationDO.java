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
 * 建站初始化注册记录: 某个域把自己登记到 4A 中心, ctc 据此感知并编排.
 */
@BizModel(name = "建站初始化域注册", module = "z-ctc", description = "建站初始化域注册.")
@TableName("z_ctc_site_init_registration")
public class SiteInitRegistrationDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("domain_code")
    @BizField(name = "域编码 (全平台唯一)", type = BizField.FieldType.STRING)
    private String domainCode;

    @TableField("domain_name")
    @BizField(name = "域名称", type = BizField.FieldType.STRING)
    private String domainName;

    @TableField("order_no")
    @BizField(name = "跨域执行顺序, 数字小者先跑", type = BizField.FieldType.INTEGER)
    private Integer orderNo;

    @TableField("endpoint")
    @BizField(name = "域初始化服务根地址", type = BizField.FieldType.STRING)
    private String endpoint;

    @TableField("protocol_version")
    @BizField(name = "协议版本", type = BizField.FieldType.STRING)
    private String protocolVersion;

    @TableField("capabilities")
    @BizField(name = "能力编码清单 (JSON 数组)", type = BizField.FieldType.STRING)
    private String capabilities;

    @TableField("status")
    @BizField(name = "状态: 0=停用 1=启用", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("registered_at")
    @BizField(name = "最近注册时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime registeredAt;

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

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
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
