package com.zifang.ctc.core.approval;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点展开快照：发起时宿主把模板节点具体化为审批人集合，审批时按节点步进。
 * JSON 字段名与宿主侧（z-kb）的快照结构保持一致，跨服务往返不改名。
 */
public class ApprovalNodeStep {

    public static final String STATE_PENDING = "PENDING";
    public static final String STATE_WAITING = "WAITING";
    public static final String STATE_APPROVED = "APPROVED";
    public static final String STATE_REJECTED = "REJECTED";

    public static final String MODE_SEQUENTIAL = "SEQUENTIAL";
    public static final String MODE_ANY = "ANY";

    private String name;
    private String approverType;
    private String mode;
    private String approverRoleCode;
    private String approverRoleName;
    private List<String> approverIds = new ArrayList<>();
    private List<String> approverNames = new ArrayList<>();
    private String state;
    private String comment;
    private Instant actedAt;
    private int actedCount;

    public String displayName() {
        if (approverNames != null && !approverNames.isEmpty()) {
            return String.join(" / ", approverNames);
        }
        if (approverRoleName != null && !approverRoleName.isBlank()) {
            return approverRoleName;
        }
        return hasText(name) ? name : "";
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApproverType() { return approverType; }
    public void setApproverType(String approverType) { this.approverType = approverType; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getApproverRoleCode() { return approverRoleCode; }
    public void setApproverRoleCode(String approverRoleCode) { this.approverRoleCode = approverRoleCode; }
    public String getApproverRoleName() { return approverRoleName; }
    public void setApproverRoleName(String approverRoleName) { this.approverRoleName = approverRoleName; }
    public List<String> getApproverIds() { return approverIds; }
    public void setApproverIds(List<String> approverIds) {
        this.approverIds = approverIds == null ? new ArrayList<>() : approverIds;
    }
    public List<String> getApproverNames() { return approverNames; }
    public void setApproverNames(List<String> approverNames) {
        this.approverNames = approverNames == null ? new ArrayList<>() : approverNames;
    }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getActedAt() { return actedAt; }
    public void setActedAt(Instant actedAt) { this.actedAt = actedAt; }
    public int getActedCount() { return actedCount; }
    public void setActedCount(int actedCount) { this.actedCount = actedCount; }
}
