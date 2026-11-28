package com.zifang.ctc.core.approval;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.core.approval.entity.ApprovalTemplateDO;
import com.zifang.ctc.core.approval.entity.ApprovalTicketDO;
import com.zifang.ctc.core.approval.mapper.ApprovalTemplateMapper;
import com.zifang.ctc.core.approval.mapper.ApprovalTicketMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 权限类审批引擎（4A 范畴）：模板 + 票生命周期 + 节点步进的真源。
 *
 * <p>边界（2026-11-28 拍板）：权限相关的审批归 z-ctc；更复杂的业务审批归 z-wf
 * （审批中心/Camunda），本引擎不自建会签/表单/超时流转这类重流程。引擎对
 * bizType/授权副作用完全透明——业务语义（知识库 ACL、域成员等授权落地）由宿主
 * 按 bizType 自己解释；宿主在发起时把模板节点展开为审批人集合（展开逻辑留在
 * 宿主，因为审批人候选来自宿主的业务表）。</p>
 */
@Service("ctcApprovalEngine")
public class ApprovalEngine {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String VIEW_MINE = "mine";
    public static final String VIEW_TODO = "todo";
    public static final String VIEW_ALL = "all";

    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private final ApprovalTemplateMapper templateMapper;
    private final ApprovalTicketMapper ticketMapper;

    public ApprovalEngine(ApprovalTemplateMapper templateMapper, ApprovalTicketMapper ticketMapper) {
        this.templateMapper = templateMapper;
        this.ticketMapper = ticketMapper;
    }

    /* ------------------------------------------------------------------ 模板 */

    public List<ApprovalTemplateDO> listTemplates(String tenant) {
        return templateMapper.selectList(new QueryWrapper<ApprovalTemplateDO>()
                .eq("tenant_code", tenant).eq("is_deleted", 0).orderByAsc("create_time"));
    }

    @Transactional(rollbackFor = Exception.class)
    public ApprovalTemplateDO saveTemplate(String tenant, String actorId, String code, String name,
                                           String bizType, String nodesJson) {
        if (!hasText(tenant) || !hasText(code) || !hasText(name)) {
            throw new ApprovalException(ApprovalException.INVALID_PARAM, "租户与模板编码、名称必填");
        }
        if (!hasText(nodesJson)) {
            throw new ApprovalException(ApprovalException.INVALID_PARAM, "模板节点快照必填");
        }
        String biz = bizType == null || bizType.trim().isEmpty() ? "GENERIC" : bizType.trim();
        List<ApprovalTemplateDO> existing = templateMapper.selectList(new QueryWrapper<ApprovalTemplateDO>()
                .eq("tenant_code", tenant).eq("code", code.trim()).eq("is_deleted", 0).last("LIMIT 1"));
        Instant now = Instant.now();
        ApprovalTemplateDO entity = existing.isEmpty() ? new ApprovalTemplateDO() : existing.get(0);
        entity.setTenantCode(tenant);
        entity.setCode(code.trim());
        entity.setName(name.trim());
        if (entity.getVersion() == null || entity.getVersion().isBlank()) {
            entity.setVersion("v1");
        }
        entity.setBizType(biz);
        entity.setNodesJson(nodesJson);
        entity.setPublishedAt(now);
        entity.setPublishedBy(actorId);
        entity.setIsDeleted(0);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateBy(actorId);
            entity.setCreateTime(now);
        }
        entity.setUpdateBy(actorId);
        entity.setUpdateTime(now);
        if (existing.isEmpty()) {
            templateMapper.insert(entity);
        } else {
            templateMapper.updateById(entity);
        }
        return entity;
    }

    /** 宿主缺省模板按需播种（幂等）：宿主自己 diff 缺哪些，逐个 saveTemplate 即可。 */

    public ApprovalTemplateDO requireTemplate(String tenant, String code) {
        List<ApprovalTemplateDO> rows = templateMapper.selectList(new QueryWrapper<ApprovalTemplateDO>()
                .eq("tenant_code", tenant).eq("code", code).eq("is_deleted", 0).last("LIMIT 1"));
        if (rows.isEmpty()) {
            throw new ApprovalException(ApprovalException.TEMPLATE_NOT_FOUND, "审批模板不存在: " + code);
        }
        return rows.get(0);
    }

    /* ------------------------------------------------------------------ 票 */

    /** 发起：宿主已展开好的节点快照直接入票。 */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalTicketDO apply(String tenant, String actorId, String actorName, String templateCode,
                                  String bizType, String bizRef, String bizName, String bizRole,
                                  String title, String reason, List<String> resourceCodes,
                                  List<ApprovalNodeStep> steps) {
        if (!hasText(bizType)) {
            throw new ApprovalException(ApprovalException.INVALID_PARAM, "业务类型必填");
        }
        if (!hasText(templateCode)) {
            throw new ApprovalException(ApprovalException.INVALID_PARAM, "审批模板必填");
        }
        ApprovalTemplateDO template = requireTemplate(tenant, templateCode);
        if (steps == null || steps.isEmpty()) {
            throw new ApprovalException(ApprovalException.INVALID_PARAM, "审批节点快照必填");
        }
        Instant now = Instant.now();
        ApprovalTicketDO entity = new ApprovalTicketDO();
        entity.setId(UUID.randomUUID().toString());
        entity.setTenantCode(tenant);
        entity.setCode("AP" + System.currentTimeMillis() + (100 + (int) (Math.random() * 900)));
        entity.setTitle(hasText(title) ? title.trim() : "审批申请");
        entity.setTemplateCode(template.getCode());
        entity.setTemplateName(template.getName());
        entity.setBizType(bizType.trim());
        entity.setBizRef(hasText(bizRef) ? bizRef.trim() : null);
        entity.setBizName(bizName);
        entity.setBizRole(hasText(bizRole) ? bizRole.trim() : null);
        entity.setApplicantId(actorId);
        entity.setApplicantName(actorName);
        entity.setResourceCodesJson(resourceCodes == null || resourceCodes.isEmpty()
                ? null : toJson(resourceCodes));
        entity.setStatus(STATUS_PENDING);
        entity.setCurrentNode(0);
        entity.setCurrentApproverName(steps.get(0).displayName());
        entity.setReason(hasText(reason) ? reason.trim() : null);
        entity.setNodesJson(toJson(steps));
        entity.setIsDeleted(0);
        entity.setCreateBy(actorId);
        entity.setUpdateBy(actorId);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        ticketMapper.insert(entity);
        return entity;
    }

    public List<ApprovalTicketDO> listTickets(String tenant, String view, String actorId,
                                              String status, String keyword) {
        String v = view == null || view.trim().isEmpty() ? VIEW_MINE : view.trim();
        List<ApprovalTicketDO> rows = ticketMapper.selectList(new QueryWrapper<ApprovalTicketDO>()
                .eq("tenant_code", tenant).eq("is_deleted", 0).orderByDesc("create_time"));
        List<ApprovalTicketDO> out = new ArrayList<>();
        for (ApprovalTicketDO row : rows) {
            if (VIEW_MINE.equals(v) && !sameActor(row.getApplicantId(), actorId)) {
                continue;
            }
            if (VIEW_TODO.equals(v) && !isTodo(row, actorId)) {
                continue;
            }
            if (hasText(status) && !status.trim().equals(row.getStatus())) {
                continue;
            }
            if (hasText(keyword) && !matchesKeyword(row, keyword.trim())) {
                continue;
            }
            out.add(row);
        }
        return out;
    }

    public ApprovalTicketDO requireTicket(String tenant, String id) {
        if (!hasText(id)) {
            throw new ApprovalException(ApprovalException.TICKET_NOT_FOUND, "审批票标识必填");
        }
        List<ApprovalTicketDO> rows = ticketMapper.selectList(new QueryWrapper<ApprovalTicketDO>()
                .eq("tenant_code", tenant).eq("id", id).eq("is_deleted", 0).last("LIMIT 1"));
        if (rows.isEmpty()) {
            throw new ApprovalException(ApprovalException.TICKET_NOT_FOUND, "审批票不存在: " + id);
        }
        return rows.get(0);
    }

    /** 审批动作：approve / reject。语义与宿主迁移前的引擎一致（ANY 或签 / SEQUENTIAL 顺签）。 */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalTicketDO act(String tenant, String actorId, String id, boolean approve, String comment) {
        ApprovalTicketDO entity = requireTicket(tenant, id);
        if (!STATUS_PENDING.equals(entity.getStatus())) {
            throw new ApprovalException(ApprovalException.STATE_CONFLICT, "仅待审批票可审批: " + entity.getCode());
        }
        List<ApprovalNodeStep> steps = parseSteps(entity.getNodesJson());
        int current = entity.getCurrentNode() == null ? 0 : entity.getCurrentNode();
        if (current < 0 || current >= steps.size()) {
            throw new ApprovalException(ApprovalException.STATE_CONFLICT, "审批节点已越界: " + entity.getCode());
        }
        ApprovalNodeStep step = steps.get(current);
        if (!ApprovalNodeStep.STATE_PENDING.equals(step.getState())
                || !step.getApproverIds().contains(actorId)) {
            throw new ApprovalException(ApprovalException.PRINCIPAL_INVALID, "当前节点不待您审批");
        }
        Instant now = Instant.now();
        boolean sequentialMulti = ApprovalNodeStep.MODE_SEQUENTIAL.equals(step.getMode())
                && step.getActedCount() + 1 < step.getApproverIds().size();
        step.setActedCount(step.getActedCount() + 1);
        step.setComment(hasText(comment) ? comment.trim() : null);
        step.setActedAt(now);
        if (approve) {
            if (sequentialMulti) {
                entity.setCurrentApproverName(step.displayName());
            } else {
                step.setState(ApprovalNodeStep.STATE_APPROVED);
                int next = current + 1;
                if (next >= steps.size()) {
                    entity.setStatus(STATUS_APPROVED);
                    entity.setFinishedAt(now);
                } else {
                    entity.setCurrentNode(next);
                    steps.get(next).setState(ApprovalNodeStep.STATE_PENDING);
                    entity.setCurrentApproverName(steps.get(next).displayName());
                }
            }
        } else {
            step.setState(ApprovalNodeStep.STATE_REJECTED);
            entity.setStatus(STATUS_REJECTED);
            entity.setFinishedAt(now);
        }
        entity.setNodesJson(toJson(steps));
        entity.setUpdateBy(actorId);
        entity.setUpdateTime(now);
        ticketMapper.updateById(entity);
        return entity;
    }

    /* ------------------------------------------------------------------ 内部 */

    /** 该票当前是否轮到 actorId 审批（todo 判定，控制器视图用）。 */
    public boolean isTodoOf(ApprovalTicketDO row, String actorId) {
        return isTodo(row, actorId);
    }

    private boolean isTodo(ApprovalTicketDO row, String actorId) {
        if (!STATUS_PENDING.equals(row.getStatus()) || !hasText(actorId)) {
            return false;
        }
        List<ApprovalNodeStep> steps = parseSteps(row.getNodesJson());
        int current = row.getCurrentNode() == null ? 0 : row.getCurrentNode();
        if (current < 0 || current >= steps.size()) {
            return false;
        }
        return steps.get(current).getApproverIds().contains(actorId);
    }

    private boolean matchesKeyword(ApprovalTicketDO row, String keyword) {
        return contains(row.getTitle(), keyword) || contains(row.getCode(), keyword)
                || contains(row.getTemplateName(), keyword);
    }

    private static boolean sameActor(String applicantId, String actorId) {
        return applicantId != null && applicantId.equals(actorId);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    public List<ApprovalNodeStep> parseSteps(String json) {
        if (!hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<ApprovalNodeStep> steps = JSON.readValue(json,
                    new TypeReference<List<ApprovalNodeStep>>() { });
            return steps == null ? new ArrayList<>() : steps;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new ApprovalException(ApprovalException.INVALID_PARAM, "节点快照序列化失败", e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
