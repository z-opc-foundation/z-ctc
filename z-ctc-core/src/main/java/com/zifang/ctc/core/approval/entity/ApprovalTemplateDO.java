package com.zifang.ctc.core.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

/** 审批模板（z_ctc_approval_template）：权限类审批的模板真源。 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@TableName("z_ctc_approval_template")
public class ApprovalTemplateDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tenantCode;
    private String code;
    private String name;
    private String version;
    private String bizType;
    /** 模板节点 JSON（节点由宿主按自己的审批人语义解释）。 */
    private String nodesJson;
    private Instant publishedAt;
    private String publishedBy;
    private Integer isDeleted;
    private String createBy;
    private String updateBy;
    private Instant createTime;
    private Instant updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getNodesJson() { return nodesJson; }
    public void setNodesJson(String nodesJson) { this.nodesJson = nodesJson; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Instant getCreateTime() { return createTime; }
    public void setCreateTime(Instant createTime) { this.createTime = createTime; }
    public Instant getUpdateTime() { return updateTime; }
    public void setUpdateTime(Instant updateTime) { this.updateTime = updateTime; }
}
