package com.zifang.ctc.core.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

/**
 * 审批票（z_ctc_approval_ticket）：权限类审批的票真源。业务类型/引用对引擎透明——
 * 引擎只管模板 + 节点步进 + 决策；批准后的授权副作用由宿主按 bizType 自己落。
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@TableName("z_ctc_approval_ticket")
public class ApprovalTicketDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tenantCode;
    private String code;
    private String title;
    private String templateCode;
    private String templateName;
    private String bizType;
    private String bizRef;
    private String bizName;
    private String bizRole;
    private String applicantId;
    private String applicantName;
    private String resourceCodesJson;
    private String status;
    private Integer currentNode;
    private String currentApproverName;
    private String reason;
    /** 节点展开快照 JSON（发起时由宿主展开好审批人集合）。 */
    private String nodesJson;
    private Instant finishedAt;
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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizRef() { return bizRef; }
    public void setBizRef(String bizRef) { this.bizRef = bizRef; }
    public String getBizName() { return bizName; }
    public void setBizName(String bizName) { this.bizName = bizName; }
    public String getBizRole() { return bizRole; }
    public void setBizRole(String bizRole) { this.bizRole = bizRole; }
    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getResourceCodesJson() { return resourceCodesJson; }
    public void setResourceCodesJson(String resourceCodesJson) { this.resourceCodesJson = resourceCodesJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCurrentNode() { return currentNode; }
    public void setCurrentNode(Integer currentNode) { this.currentNode = currentNode; }
    public String getCurrentApproverName() { return currentApproverName; }
    public void setCurrentApproverName(String currentApproverName) { this.currentApproverName = currentApproverName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNodesJson() { return nodesJson; }
    public void setNodesJson(String nodesJson) { this.nodesJson = nodesJson; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
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
