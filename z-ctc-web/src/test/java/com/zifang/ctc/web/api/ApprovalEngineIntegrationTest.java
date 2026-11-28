package com.zifang.ctc.web.api;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.ctc.core.approval.ApprovalEngine;
import com.zifang.ctc.core.approval.ApprovalException;
import com.zifang.ctc.core.approval.ApprovalNodeStep;
import com.zifang.ctc.core.approval.entity.ApprovalTicketDO;
import com.zifang.ctc.core.approval.mapper.ApprovalTicketMapper;
import com.zifang.ctc.core.approval.mapper.ApprovalTemplateMapper;
import com.zifang.util.json.JsonUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权限类审批引擎（H2 + 真实 MyBatis Mapper）。
 *
 * <p>口径（2026-11-28 审批分家）：模板 + 票流转 + 决策的真源在 z-ctc；授权副作用
 * 由宿主按 bizType 落地，引擎对业务语义透明。守卫：状态机（ANY/SEQUENTIAL）、
 * 审批人资格、状态冲突、租户隔离、模板缺省播种由宿主 saveTemplate 幂等完成。</p>
 */
class ApprovalEngineIntegrationTest {

    private JdbcTemplate jdbc;
    private ApprovalEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:ctc_apr_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        jdbc = new JdbcTemplate(dataSource);
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(
                new ClassPathResource("db/h2/z-ctc-approval.sql"));
        schema.setSqlScriptEncoding("UTF-8");
        schema.execute(dataSource);

        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(ApprovalTemplateMapper.class);
        configuration.addMapper(ApprovalTicketMapper.class);
        factory.setConfiguration(configuration);
        SqlSessionFactory sqlSessionFactory = factory.getObject();
        SqlSession session = sqlSessionFactory.openSession(true);
        engine = new ApprovalEngine(session.getMapper(ApprovalTemplateMapper.class),
                session.getMapper(ApprovalTicketMapper.class));
    }

    @Test
    void templateUpsertIsIdempotentPerTenant() {
        engine.saveTemplate("tenant-a", "system", "TMPL_T", "模板T", "KB_ACCESS", stepsJson("平台管理员审批", "admin"));
        engine.saveTemplate("tenant-a", "system", "TMPL_T", "模板T改", "KB_ACCESS", stepsJson("平台管理员审批", "admin"));
        assertEquals(1, countTemplates("tenant-a", "TMPL_T"));
        assertEquals("模板T改", engine.requireTemplate("tenant-a", "TMPL_T").getName());
        assertThrows(ApprovalException.class, () -> engine.requireTemplate("tenant-b", "TMPL_T"));
    }

    @Test
    void anyModeApproveFlowReachesApprovedAndHostDoesGrants() {
        seed("TMPL_KB", "KB_ACCESS", "知识库管理员审批", "kb-admin-1", "kb-admin-2");
        ApprovalTicketDO ticket = engine.apply("tenant-a", "alice", "Alice", "TMPL_KB",
                "KB_ACCESS", "kb-1", "演示库", "WRITE", null, "干活要写权限", null,
                steps("知识库管理员审批", "kb-admin-1", "kb-admin-2"));
        assertEquals("PENDING", ticket.getStatus());

        ApprovalTicketDO acted = engine.act("tenant-a", "kb-admin-1", ticket.getId(), true, "同意");
        assertEquals("APPROVED", acted.getStatus());
        assertNotNull(acted.getFinishedAt());
        // 决策真源到此为止；授权副作用由宿主按 bizType 落（引擎不落 ACL/成员）
        assertEquals(0, countTable("z_ctc_approval_ticket WHERE status <> 'APPROVED'"));
    }

    @Test
    void sequentialModeRequiresEveryApproverInOrder() {
        seed("TMPL_SEQ", "GENERIC", "第一节点", "approver-1", null);
        List<ApprovalNodeStep> steps = twoSequentialSteps();
        ApprovalTicketDO ticket = engine.apply("tenant-a", "alice", "Alice", "TMPL_SEQ",
                "GENERIC", "res-1", null, null, null, null, null, steps);
        assertEquals("PENDING", ticket.getStatus());

        assertThrows(ApprovalException.class,
                () -> engine.act("tenant-a", "approver-2", ticket.getId(), true, null));
        engine.act("tenant-a", "approver-1", ticket.getId(), true, null);
        ApprovalTicketDO done = engine.act("tenant-a", "approver-2", ticket.getId(), true, null);
        assertEquals("APPROVED", done.getStatus());
    }

    @Test
    void rejectEndsTicketAndBlocksFurtherActions() {
        seed("TMPL_R", "KB_ACCESS", "知识库管理员审批", "kb-admin-1", null);
        ApprovalTicketDO ticket = engine.apply("tenant-a", "alice", "Alice", "TMPL_R",
                "KB_ACCESS", "kb-2", "演示库2", "READ", null, null, null,
                steps("知识库管理员审批", "kb-admin-1"));
        engine.act("tenant-a", "kb-admin-1", ticket.getId(), false, "暂不开放");
        assertEquals("REJECTED", engine.requireTicket("tenant-a", ticket.getId()).getStatus());
        assertThrows(ApprovalException.class,
                () -> engine.act("tenant-a", "kb-admin-1", ticket.getId(), true, null));
    }

    @Test
    void strangerCannotActAndTenantIsolated() {
        seed("TMPL_S", "KB_ACCESS", "知识库管理员审批", "kb-admin-1", null);
        ApprovalTicketDO ticket = engine.apply("tenant-a", "alice", "Alice", "TMPL_S",
                "KB_ACCESS", "kb-3", "演示库3", "READ", null, null, null,
                steps("知识库管理员审批", "kb-admin-1"));
        assertThrows(ApprovalException.class,
                () -> engine.act("tenant-a", "stranger", ticket.getId(), true, null));
        assertThrows(ApprovalException.class,
                () -> engine.requireTicket("tenant-b", ticket.getId()));
    }

    @Test
    void viewsFilterMineTodoAll() {
        seed("TMPL_V", "KB_ACCESS", "知识库管理员审批", "kb-admin-1", null);
        engine.apply("tenant-a", "alice", "Alice", "TMPL_V", "KB_ACCESS", "kb-4",
                "演示库4", "WRITE", null, null, null, steps("知识库管理员审批", "kb-admin-1"));
        assertEquals(1, engine.listTickets("tenant-a", "mine", "alice", null, null).size());
        assertEquals(1, engine.listTickets("tenant-a", "todo", "kb-admin-1", null, null).size());
        assertEquals(0, engine.listTickets("tenant-a", "todo", "stranger", null, null).size());
        assertEquals(1, engine.listTickets("tenant-a", "all", "kb-admin-1", null, null).size());
        assertTrue(engine.isTodoOf(engine.listTickets("tenant-a", "todo", "kb-admin-1", null, null).get(0), "kb-admin-1"));
    }

    /* ------------------------------------------------------------------ 造数 */

    private void seed(String code, String bizType, String stepName, String approver1, String approver2) {
        engine.saveTemplate("tenant-a", "system", code, code + "模板", bizType,
                stepsJson(stepName, approver1, approver2));
    }

    private String stepsJson(String stepName, String... approvers) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("name", stepName);
        node.put("mode", "ANY");
        List<String> ids = new ArrayList<>();
        for (String approver : approvers) {
            if (approver != null) {
                ids.add(approver);
            }
        }
        node.put("approverIds", ids);
        nodes.add(node);
        return JsonUtil.toJson(nodes);
    }

    private List<ApprovalNodeStep> steps(String stepName, String... approvers) {
        List<ApprovalNodeStep> steps = new ArrayList<>();
        ApprovalNodeStep step = new ApprovalNodeStep();
        step.setName(stepName);
        step.setMode(ApprovalNodeStep.MODE_ANY);
        List<String> ids = new ArrayList<>();
        for (String approver : approvers) {
            if (approver != null) {
                ids.add(approver);
            }
        }
        step.setApproverIds(ids);
        step.setApproverNames(new ArrayList<>(ids));
        step.setState(ApprovalNodeStep.STATE_PENDING);
        steps.add(step);
        return steps;
    }

    private List<ApprovalNodeStep> twoSequentialSteps() {
        List<ApprovalNodeStep> steps = new ArrayList<>();
        ApprovalNodeStep first = new ApprovalNodeStep();
        first.setName("第一节点");
        first.setMode(ApprovalNodeStep.MODE_ANY);
        first.setApproverIds(new ArrayList<>(Collections.singletonList("approver-1")));
        first.setApproverNames(new ArrayList<>(Collections.singletonList("approver-1")));
        first.setState(ApprovalNodeStep.STATE_PENDING);
        steps.add(first);
        ApprovalNodeStep second = new ApprovalNodeStep();
        second.setName("第二节点");
        second.setMode(ApprovalNodeStep.MODE_SEQUENTIAL);
        second.setApproverIds(new ArrayList<>(Collections.singletonList("approver-2")));
        second.setApproverNames(new ArrayList<>(Collections.singletonList("approver-2")));
        second.setState(ApprovalNodeStep.STATE_WAITING);
        steps.add(second);
        return steps;
    }

    private int countTemplates(String tenant, String code) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM z_ctc_approval_template WHERE tenant_code=? AND code=?",
                Integer.class, tenant, code);
        return n == null ? 0 : n;
    }

    private int countTable(String suffix) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM z_ctc_approval_ticket " + suffix, Integer.class);
        return n == null ? 0 : n;
    }
}
