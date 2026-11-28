package com.zifang.ctc.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.core.ak.AkCaller;
import com.zifang.ctc.core.approval.ApprovalEngine;
import com.zifang.ctc.core.approval.ApprovalNodeStep;
import com.zifang.ctc.core.approval.ApprovalException;
import com.zifang.ctc.core.approval.entity.ApprovalTemplateDO;
import com.zifang.ctc.core.approval.entity.ApprovalTicketDO;
import com.zifang.util.core.meta.Result;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizParam;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限类审批 REST 面（4A 范畴）。
 *
 * <p>2026-11-28 审批分家拍板：权限相关的审批真源在 z-ctc（本控制器）；
 * 更复杂的业务审批归 z-wf，本引擎不自建重流程。宿主（业务服务）在发起时
 * 已把模板节点展开为审批人集合，这里只管模板存储 + 票流转 + 决策；
 * 批准后的授权副作用由宿主按 bizType 自己落地。</p>
 */
@RestController("ctcApprovalV1Controller")
@RequestMapping("/api/ctc/v1/approval")
public class ApprovalController {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ApprovalEngine engine;
    private final AkCallerResolver callerResolver;

    public ApprovalController(ApprovalEngine engine, AkCallerResolver callerResolver) {
        this.engine = engine;
        this.callerResolver = callerResolver;
    }

    @PostMapping("/templates/list")
    @BizApi(summary = "审批模板清单", description = "权限类审批模板真源清单。POST /api/ctc/v1/approval/templates/list", tags = { "z-ctc" })
    public Result<List<Map<String, Object>>> templates(@RequestBody(required = false) Map<String, Object> body,
                                                       HttpServletRequest request) {
        AkCaller caller = callerResolver.resolve(request);
        List<Map<String, Object>> out = new ArrayList<>();
        for (ApprovalTemplateDO row : engine.listTemplates(caller.getTenantCode())) {
            out.add(templateView(row));
        }
        return Result.success(out);
    }

    @PostMapping("/templates/save")
    @BizApi(summary = "保存审批模板", description = "按编码 upsert；宿主缺省模板播种也走这里。POST /api/ctc/v1/approval/templates/save", tags = { "z-ctc" })
    public Result<Map<String, Object>> saveTemplate(@RequestBody @BizParam(name = "body", type = com.zifang.z.meta.api.annotation.BizField.FieldType.JSON,
            description = "", position = com.zifang.z.meta.api.annotation.BizParam.Position.BODY) TemplateSave body,
                                                    HttpServletRequest request) {
        AkCaller caller = callerResolver.resolve(request);
        try {
            ApprovalTemplateDO saved = engine.saveTemplate(caller.getTenantCode(), caller.getActorId(),
                    body == null ? null : body.getCode(), body == null ? null : body.getName(),
                    body == null ? null : body.getBizType(), toJson(body == null ? null : body.getNodes()));
            return Result.success(templateView(saved));
        } catch (ApprovalException ex) {
            return Result.<Map<String, Object>>fail(ex.getMessage()).code(ex.getCode());
        }
    }

    @PostMapping("/tickets/apply")
    @BizApi(summary = "发起审批", description = "宿主已展开审批人集合的节点快照直接入票。POST /api/ctc/v1/approval/tickets/apply", tags = { "z-ctc" })
    public Result<Map<String, Object>> apply(@RequestBody @BizParam(name = "body", type = com.zifang.z.meta.api.annotation.BizField.FieldType.JSON,
            description = "", position = com.zifang.z.meta.api.annotation.BizParam.Position.BODY) TicketApply body,
                                             HttpServletRequest request) {
        AkCaller caller = callerResolver.resolve(request);
        try {
            ApprovalTicketDO saved = engine.apply(caller.getTenantCode(), caller.getActorId(), caller.getActorName(),
                    body == null ? null : body.getTemplateCode(),
                    body == null ? null : body.getBizType(),
                    body == null ? null : body.getBizRef(),
                    body == null ? null : body.getBizName(),
                    body == null ? null : body.getBizRole(),
                    body == null ? null : body.getTitle(),
                    body == null ? null : body.getReason(),
                    body == null ? null : body.getResourceCodes(),
                    body == null ? null : parseSteps(body.getSteps()));
            return Result.success(ticketView(saved, caller.getActorId()));
        } catch (ApprovalException ex) {
            return Result.<Map<String, Object>>fail(ex.getMessage()).code(ex.getCode());
        }
    }

    @PostMapping("/tickets/page")
    @BizApi(summary = "审批票分页", description = "view mine/todo/all + 状态/关键字过滤；分页在调用方内存页。POST /api/ctc/v1/approval/tickets/page", tags = { "z-ctc" })
    public Result<Map<String, Object>> page(@RequestBody(required = false) @BizParam(name = "body", type = com.zifang.z.meta.api.annotation.BizField.FieldType.JSON,
            description = "", position = com.zifang.z.meta.api.annotation.BizParam.Position.BODY) TicketPageQuery body,
                                            HttpServletRequest request) {
        AkCaller caller = callerResolver.resolve(request);
        List<ApprovalTicketDO> rows = engine.listTickets(caller.getTenantCode(),
                body == null ? null : body.getView(), caller.getActorId(),
                body == null ? null : body.getStatus(), body == null ? null : body.getKeyword());
        long total = rows.size();
        long current = body == null || body.getCurrent() == null || body.getCurrent() <= 0 ? 1 : body.getCurrent();
        long size = body == null || body.getSize() == null || body.getSize() <= 0 ? 20 : Math.min(body.getSize(), 100);
        int from = (int) Math.min((current - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        List<Map<String, Object>> records = new ArrayList<>();
        for (ApprovalTicketDO row : rows.subList(from, to)) {
            records.add(ticketView(row, caller.getActorId()));
        }
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("current", current);
        page.put("size", size);
        page.put("total", total);
        page.put("records", records);
        return Result.success(page);
    }

    @GetMapping("/tickets/detail")
    @BizApi(summary = "审批票详情", description = "含节点明细与 canApprove 标记。GET /api/ctc/v1/approval/tickets/detail?id=", tags = { "z-ctc" })
    public Result<Map<String, Object>> detail(@RequestParam("id") String id, HttpServletRequest request) {
        AkCaller caller = callerResolver.resolve(request);
        try {
            return Result.success(ticketView(engine.requireTicket(caller.getTenantCode(), id), caller.getActorId()));
        } catch (ApprovalException ex) {
            return Result.<Map<String, Object>>fail(ex.getMessage()).code(ex.getCode());
        }
    }

    @PostMapping("/tickets/act")
    @BizApi(summary = "审批动作", description = "approve=true 通过 / false 驳回；节点步进与状态冲突由引擎裁决。POST /api/ctc/v1/approval/tickets/act", tags = { "z-ctc" })
    public Result<Map<String, Object>> act(@RequestBody @BizParam(name = "body", type = com.zifang.z.meta.api.annotation.BizField.FieldType.JSON,
            description = "", position = com.zifang.z.meta.api.annotation.BizParam.Position.BODY) TicketAct body,
                                           HttpServletRequest request) {
        AkCaller caller = callerResolver.resolve(request);
        if (body == null || body.getApprove() == null) {
            throw new ApprovalException(ApprovalException.INVALID_PARAM, "审批动作必填");
        }
        try {
            ApprovalTicketDO saved = engine.act(caller.getTenantCode(), caller.getActorId(),
                    body.getId(), body.getApprove(), body.getComment());
            return Result.success(ticketView(saved, caller.getActorId()));
        } catch (ApprovalException ex) {
            return Result.<Map<String, Object>>fail(ex.getMessage()).code(ex.getCode());
        }
    }

    /* ------------------------------------------------------------------ 视图与 DTO */

    private Map<String, Object> templateView(ApprovalTemplateDO row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("code", row.getCode());
        out.put("name", row.getName());
        out.put("version", row.getVersion());
        out.put("bizType", row.getBizType());
        out.put("nodes", readTree(row.getNodesJson()));
        out.put("publishedAt", row.getPublishedAt() == null ? null : row.getPublishedAt().toEpochMilli());
        out.put("publishedBy", row.getPublishedBy());
        return out;
    }

    private Map<String, Object> ticketView(ApprovalTicketDO row, String actorId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("code", row.getCode());
        out.put("title", row.getTitle());
        out.put("templateCode", row.getTemplateCode());
        out.put("templateName", row.getTemplateName());
        out.put("bizType", row.getBizType());
        out.put("bizRef", row.getBizRef());
        out.put("bizName", row.getBizName());
        out.put("bizRole", row.getBizRole());
        out.put("applicantId", row.getApplicantId());
        out.put("applicantName", row.getApplicantName());
        out.put("resourceCodes", readTree(row.getResourceCodesJson()));
        out.put("status", row.getStatus());
        out.put("currentNode", row.getCurrentNode());
        out.put("currentApproverName", row.getCurrentApproverName());
        out.put("reason", row.getReason());
        out.put("finishedAt", row.getFinishedAt() == null ? null : row.getFinishedAt().toEpochMilli());
        out.put("createTime", row.getCreateTime() == null ? null : row.getCreateTime().toEpochMilli());
        out.put("nodes", readTree(row.getNodesJson()));
        out.put("canApprove", engine.isTodoOf(row, actorId));
        return out;
    }

    private Object readTree(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return JSON.readValue(json, Object.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<ApprovalNodeStep> parseSteps(Object raw) {
        if (raw == null) {
            return new ArrayList<>();
        }
        return JSON.convertValue(raw,
                JSON.getTypeFactory().constructCollectionType(List.class, ApprovalNodeStep.class));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new ApprovalException(ApprovalException.INVALID_PARAM, "模板节点序列化失败", e);
        }
    }

    /** 模板保存入参（nodes 由宿主按自己的节点语义给）。 */
    @com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class TemplateSave {
        private String code;
        private String name;
        private String bizType;
        private Object nodes;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBizType() { return bizType; }
        public void setBizType(String bizType) { this.bizType = bizType; }
        public Object getNodes() { return nodes; }
        public void setNodes(Object nodes) { this.nodes = nodes; }
    }

    /** 发起入参：steps 是宿主展开好的审批人集合快照。 */
    @com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class TicketApply {
        private String templateCode;
        private String bizType;
        private String bizRef;
        private String bizName;
        private String bizRole;
        private String title;
        private String reason;
        private List<String> resourceCodes;
        private List<Map<String, Object>> steps;

        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
        public String getBizType() { return bizType; }
        public void setBizType(String bizType) { this.bizType = bizType; }
        public String getBizRef() { return bizRef; }
        public void setBizRef(String bizRef) { this.bizRef = bizRef; }
        public String getBizName() { return bizName; }
        public void setBizName(String bizName) { this.bizName = bizName; }
        public String getBizRole() { return bizRole; }
        public void setBizRole(String bizRole) { this.bizRole = bizRole; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public List<String> getResourceCodes() { return resourceCodes; }
        public void setResourceCodes(List<String> resourceCodes) { this.resourceCodes = resourceCodes; }
        public List<Map<String, Object>> getSteps() { return steps; }
        public void setSteps(List<Map<String, Object>> steps) { this.steps = steps; }
    }

    @com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class TicketPageQuery {
        private String view;
        private Integer current;
        private Integer size;
        private String status;
        private String keyword;

        public String getView() { return view; }
        public void setView(String view) { this.view = view; }
        public Integer getCurrent() { return current; }
        public void setCurrent(Integer current) { this.current = current; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
    }

    @com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class TicketAct {
        private String id;
        private Boolean approve;
        private String comment;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Boolean getApprove() { return approve; }
        public void setApprove(Boolean approve) { this.approve = approve; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
