package com.zifang.ctc.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.DeptDO;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.service.DeptDbService;
import com.zifang.ctc.core.domain.service.MembershipDbService;
import com.zifang.ctc.core.domain.service.UserDbService;
import com.zifang.ctc.core.service.DeptTreeService;
import com.zifang.ctc.core.vo.DeptTreeVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 人事架构部门树服务实现.
 *
 * <p>数据来源（三表关联，均在 {@code oc} 库实测存在）：
 * <ol>
 *   <li>{@code z_ctc_ac_dept} — 部门（{@code parent_code} 为 2026-10-04 新增列）</li>
 *   <li>{@code z_ctc_ac_membership} — 租户成员，{@code dept_code} 指向部门</li>
 *   <li>{@code z_ctc_ac_user} — 人员明细</li>
 * </ol>
 *
 * <p><b>为什么不用递归</b>：部门层级来自数据，理论上可成环。递归组树在环上会
 * 栈溢出或产出重复节点，因此这里先在内存建 {@code deptCode → 节点} 索引，
 * 再按 parentCode 挂接，并对「自环 / 成环 / 指向不存在」三种情况按根降级 ——
 * <b>宁可层级错，不让接口挂掉</b>（消费方本就对查不到人员有降级分支）。
 *
 * @author zifang
 * @since 1.0.0
 */
@Service
public class AcDeptTreeServiceImpl implements DeptTreeService {

    private static final Logger log = LogManager.getLogger(AcDeptTreeServiceImpl.class);

    /** 成员状态：1=正常（对应 MembershipDO.memberStatus 注释） */
    private static final int MEMBER_STATUS_ACTIVE = 1;

    private final DeptDbService deptDbService;
    private final MembershipDbService membershipDbService;
    private final UserDbService userDbService;

    public AcDeptTreeServiceImpl(DeptDbService deptDbService,
                                 MembershipDbService membershipDbService,
                                 UserDbService userDbService) {
        this.deptDbService = deptDbService;
        this.membershipDbService = membershipDbService;
        this.userDbService = userDbService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeptTreeVO> buildDeptTree(String tenantCode, String domainCode) {
        if (isBlank(tenantCode) || isBlank(domainCode)) {
            return Collections.emptyList();
        }

        List<DeptDO> depts = listActiveDepts(tenantCode, domainCode);
        if (depts.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 建索引：deptCode → VO（LinkedHashMap 保证输出顺序稳定，便于测试与排查）
        Map<String, DeptTreeVO> byCode = new LinkedHashMap<>();
        for (DeptDO d : depts) {
            DeptTreeVO vo = new DeptTreeVO();
            vo.setId(d.getId());
            vo.setDeptCode(d.getDeptCode());
            vo.setName(d.getDeptName());
            vo.setParentCode(d.getParentCode());
            vo.setOrgCode(d.getOrgCode());
            vo.setStatus(d.getStatus());
            byCode.put(d.getDeptCode(), vo);
        }

        // 2. 挂人员：deptCode → 直属人员
        attachStaff(tenantCode, byCode);

        // 3. 组树：自环 / 成环 / 孤儿一律按根降级
        List<DeptTreeVO> roots = new ArrayList<>();
        Set<DeptTreeVO> attached = new HashSet<>();
        for (DeptDO d : depts) {
            DeptTreeVO vo = byCode.get(d.getDeptCode());
            if (vo == null) {
                continue;
            }
            String parentCode = vo.getParentCode();
            DeptTreeVO parent = isBlank(parentCode) ? null : byCode.get(parentCode);
            // 判为根的四种情况：根(parentCode 空) / 自环(父=自身) / 孤儿(父不存在) / 成环(上溯能回到自己)
            boolean isRoot = parent == null
                    || parent == vo
                    || createsCycle(vo, parent, byCode);
            if (isRoot) {
                roots.add(vo);
            } else {
                parent.getChildrenList().add(vo);
                attached.add(vo);
            }
        }

        // 4. 解析 parentId（此时父子关系已定，避免建索引时 parent 还没转成 VO）
        for (DeptDO d : depts) {
            DeptTreeVO vo = byCode.get(d.getDeptCode());
            if (vo == null || isBlank(vo.getParentCode())) {
                continue;
            }
            DeptTreeVO parent = byCode.get(vo.getParentCode());
            if (parent != null && parent != vo) {
                vo.setParentId(parent.getId());
            }
        }

        // 5. 兜底自检：任何既不在根、也没挂到任何父节点下的节点（理论上不该出现）补为根，
        //    避免"部门在库里但接口返回不到"这种静默丢数据
        for (DeptDO d : depts) {
            DeptTreeVO vo = byCode.get(d.getDeptCode());
            if (vo != null && !attached.contains(vo) && !roots.contains(vo)) {
                log.warn("[AcDeptTreeServiceImpl] 部门未挂接，兜底为根: deptCode={}", vo.getDeptCode());
                roots.add(vo);
            }
        }

        log.info("[AcDeptTreeServiceImpl] 构建部门树: tenant={}, domain={}, 部门数={}, 根节点数={}",
                tenantCode, domainCode, depts.size(), roots.size());
        return roots;
    }

    /**
     * 列出 (租户, 域) 下全部部门（含组织维度）。
     * <p>组织维度不作为过滤条件：本仓 {@code OrgController.listDepts} 也要求 orgCode，
     * 但树形消费方需要"该域下全量部门"，故此处按 (tenant, domain) 取。
     */
    private List<DeptDO> listActiveDepts(String tenantCode, String domainCode) {
        try {
            return deptDbService.selectByQuery(new QueryWrapper<DeptDO>()
                    .eq("tenant_code", tenantCode)
                    .eq("domain_code", domainCode)
                    .orderByAsc("id"));
        } catch (Exception e) {
            log.warn("[AcDeptTreeServiceImpl] 查询部门失败: {}", e.toString());
            return Collections.emptyList();
        }
    }

    /**
     * 把 membership 关联的人员挂到各节点。
     * <p>只取 {@code member_status=1}；{@code dept_code} 为 null 或指向不存在部门的成员被跳过。
     */
    private void attachStaff(String tenantCode, Map<String, DeptTreeVO> byCode) {
        List<MembershipDO> memberships;
        try {
            memberships = membershipDbService.selectByQuery(new QueryWrapper<MembershipDO>()
                    .eq("tenant_code", tenantCode)
                    .eq("member_status", MEMBER_STATUS_ACTIVE));
        } catch (Exception e) {
            log.warn("[AcDeptTreeServiceImpl] 查询成员关系失败，部门将不带人员: {}", e.toString());
            return;
        }
        if (memberships == null || memberships.isEmpty()) {
            return;
        }

        // 收集需要查明细的 userId（去重，避免 N+1）
        Set<Long> userIds = new HashSet<>();
        for (MembershipDO m : memberships) {
            if (m.getUserId() != null && !isBlank(m.getDeptCode()) && byCode.containsKey(m.getDeptCode())) {
                userIds.add(m.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, UserDO> users = new LinkedHashMap<>();
        try {
            for (UserDO u : userDbService.selectByQuery(new QueryWrapper<UserDO>()
                    .in("id", userIds))) {
                if (u != null && u.getId() != null) {
                    users.put(u.getId(), u);
                }
            }
        } catch (Exception e) {
            log.warn("[AcDeptTreeServiceImpl] 查询人员明细失败，部门将不带人员: {}", e.toString());
            return;
        }
        if (users.isEmpty()) {
            return;
        }

        for (MembershipDO m : memberships) {
            if (m.getUserId() == null || isBlank(m.getDeptCode())) {
                continue;
            }
            DeptTreeVO node = byCode.get(m.getDeptCode());
            if (node == null) {
                continue;
            }
            UserDO u = users.get(m.getUserId());
            if (u == null) {
                continue;
            }
            node.getStaffInfos().add(new DeptTreeVO.StaffVO(
                    u.getId(), u.getUserNo(), u.getUsername(), u.getNickname(), u.getEmail()));
        }
    }

    /**
     * 判断挂接 child → parent 是否会成环。
     * <p>沿 parentCode 链上溯，若能走回 child 则成环。
     */
    private boolean createsCycle(DeptTreeVO child, DeptTreeVO parent, Map<String, DeptTreeVO> byCode) {
        Set<String> visited = new HashSet<>();
        visited.add(child.getDeptCode());
        String cur = parent.getParentCode();
        int guard = 0;
        while (!isBlank(cur) && guard++ < byCode.size() + 1) {
            if (!visited.add(cur)) {
                return true;
            }
            DeptTreeVO node = byCode.get(cur);
            if (node == null) {
                return false;
            }
            cur = node.getParentCode();
        }
        // 走满上限仍未回到 child：视为成环（防御性，避免极端脏数据下无限上溯）
        return guard > byCode.size();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
