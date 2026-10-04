package com.zifang.ctc.core.service;

import com.zifang.ctc.core.vo.DeptTreeVO;

import java.util.List;

/**
 * 人事架构部门树服务.
 *
 * <p><b>2026-10-04 新增。</b>z-ctc 原有 {@link DeptService} 只提供<b>平铺 CRUD</b>
 * （按 orgCode 列出所有部门），无法表达层级；而消费方 z-opc 的
 * {@code TeamStaffAdapter.fetchOrgTree} 需要的是<b>带人员的嵌套树</b>。
 * 该端点（{@code /ctc-osc/dept/getStaffDeptTree}）此前<b>本仓从未定义</b>。
 *
 * @author zifang
 * @since 1.0.0
 */
public interface DeptTreeService {

    /**
     * 构建指定 (租户, 域) 下的部门树，节点挂直属人员。
     *
     * <p>层级依据 {@code z_ctc_ac_dept.parent_code}（2026-10-04 新增列）。
     * 人员来自 {@code z_ctc_ac_membership.dept_code → z_ctc_ac_user.id}，
     * 仅取 {@code member_status = 1}（正常）的成员。
     *
     * <p><b>孤儿与环的自愈</b>（本方法不抛异常，返回可用的降级结果）：
     * <ul>
     *   <li>{@code parent_code} 指向不存在的部门 → 该节点按<b>根</b>处理</li>
     *   <li>{@code parent_code} == 自身 → 按根处理（防自环）</li>
     *   <li>成环（A→B→A）→ 按根处理（防无限递归）</li>
     *   <li>部门无人员 → {@code staffInfos} 为空数组（不返回 null）</li>
     * </ul>
     *
     * @param tenantCode 租户编码，必填
     * @param domainCode 域编码，必填
     * @return 部门树根节点列表；无数据返回空列表（不返回 null）
     */
    List<DeptTreeVO> buildDeptTree(String tenantCode, String domainCode);
}
