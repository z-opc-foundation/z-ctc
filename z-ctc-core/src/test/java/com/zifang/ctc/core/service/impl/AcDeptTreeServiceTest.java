package com.zifang.ctc.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.DeptDO;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.service.DeptDbService;
import com.zifang.ctc.core.domain.service.MembershipDbService;
import com.zifang.ctc.core.domain.service.UserDbService;
import com.zifang.ctc.core.vo.DeptTreeVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link AcDeptTreeServiceImpl} 组树逻辑自测（内存桩，不依赖 DB）。
 *
 * <p><b>为什么必须有</b>：组树的四种降级（自环 / 成环 / 孤儿 / 无人员）
 * 在真实数据里几乎不会出现，但<b>一旦出现就是栈溢出或静默丢部门</b>。
 * 本地库 {@code z_ctc_ac_dept} 实测 0 行，无法用真实数据触发这些分支。
 *
 * <p><b>判据设计说明</b>：所有断言都用「拍平后的 deptCode 集合」比对，
 * 而不是断言树形结构本身 —— 因为组树顺序取决于 HashMap 迭代顺序，
 * 断言顺序会引入假失败。层级关系单独用 parentId 断言。
 *
 * <p>运行：javac + java（无 JUnit），见类末尾 main。
 */
public class AcDeptTreeServiceTest {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        // ── 1. 空数据 ────────────────────────────────────────────────
        eq("无部门返回空列表", 0, svc(Collections.emptyList(), emptyStaff()).buildDeptTree("t", "d").size());
        eq("空 tenantCode 返回空列表", 0, svc(Collections.emptyList(), emptyStaff()).buildDeptTree(null, "d").size());
        eq("空 domainCode 返回空列表", 0, svc(Collections.emptyList(), emptyStaff()).buildDeptTree("t", " ").size());

        // ── 2. 正常两级树 ────────────────────────────────────────────
        List<DeptDO> flat = depts(
                d(1L, "root", "总部", null),
                d(2L, "tech", "技术部", "root"),
                d(3L, "fe", "前端组", "tech"));
        List<DeptTreeVO> tree = svc(flat, emptyStaff()).buildDeptTree("t", "d");
        eq("两级树根节点数", 1, tree.size());
        eq("根是 root", "root", tree.get(0).getDeptCode());
        eq("根有 1 个子部门", 1, tree.get(0).getChildrenList().size());
        DeptTreeVO tech = tree.get(0).getChildrenList().get(0);
        eq("子部门是 tech", "tech", tech.getDeptCode());
        eq("tech 有 1 个孙部门", 1, tech.getChildrenList().size());
        eq("孙部门是 fe", "fe", tech.getChildrenList().get(0).getDeptCode());
        // parentId 解析
        eq("tech 的 parentId=1", Long.valueOf(1L), tech.getParentId());
        eq("fe 的 parentId=2", Long.valueOf(2L), tech.getChildrenList().get(0).getParentId());
        eq("根的 parentId 为 null", null, tree.get(0).getParentId());
        eq("staffInfos 默认空数组（非 null）", 0, tech.getStaffInfos().size());

        // ── 3. 自环：parent_code 指向自己 → 按根处理，不能死循环 ────
        List<DeptTreeVO> selfLoop = svc(depts(d(1L, "a", "A", "a")), emptyStaff()).buildDeptTree("t", "d");
        eq("自环不丢失节点", 1, selfLoop.size());
        eq("自环按根处理", "a", selfLoop.get(0).getDeptCode());

        // ── 4. 成环 A→B→A → 都不能丢，且不能无限递归 ─────────────────
        List<DeptTreeVO> cycle = svc(depts(
                d(1L, "a", "A", "b"),
                d(2L, "b", "B", "a")), emptyStaff()).buildDeptTree("t", "d");
        eq("成环时节点不丢", 2, flatten(cycle).size());
        eq("成环时全部可枚举", new java.util.TreeSet<>(java.util.Arrays.asList("a", "b")),
                codes(cycle));

        // ── 5. 孤儿：parent 指向不存在的部门 → 按根处理 ─────────────
        List<DeptTreeVO> orphan = svc(depts(
                d(1L, "a", "A", "ghost"),
                d(2L, "b", "B", null)), emptyStaff()).buildDeptTree("t", "d");
        eq("孤儿不丢节点", 2, orphan.size());
        eq("孤儿集合正确", new java.util.TreeSet<>(java.util.Arrays.asList("a", "b")), codes(orphan));

        // ── 6. 三级 + 多人挂同一部门 ─────────────────────────────────
        List<DeptTreeVO> staffTree = svc(
                depts(d(1L, "root", "总部", null), d(2L, "tech", "技术部", "root")),
                staffOf("tech", users(101L, 102L), "ghostDept", users(103L))).buildDeptTree("t", "d");
        DeptTreeVO root = staffTree.get(0);
        DeptTreeVO tech2 = root.getChildrenList().get(0);
        eq("tech 挂 2 人（指向不存在部门的被跳过）", 2, tech2.getStaffInfos().size());
        eq("人员 id 正确", "101", String.valueOf(tech2.getStaffInfos().get(0).getId()));
        eq("根无人员", 0, root.getStaffInfos().size());

        // ── 7. 人员指向不存在的部门 → 跳过，不影响树 ──────────────────
        List<DeptTreeVO> ghostStaff = svc(
                depts(d(1L, "a", "A", null)),
                staffOf("notexist", users(1L))).buildDeptTree("t", "d");
        eq("人员指向不存在部门不报错", 1, ghostStaff.size());
        eq("该节点无人员", 0, ghostStaff.get(0).getStaffInfos().size());

        // ── 8. 雪花 ID 无损（19 位 Long 不得丢精度）────────────────────
        String snow = "1791049245658912345";
        Long snowId = Long.parseLong(snow);
        DeptDO snowDept = new DeptDO();
        snowDept.setId(snowId);
        snowDept.setDeptCode("s");
        snowDept.setDeptName("大数据");
        snowDept.setParentCode(null);
        snowDept.setOrgCode("org");
        snowDept.setStatus(1);
        List<DeptTreeVO> snowTree = svc(
                depts(snowDept),
                staffOf("s", users(snowId))).buildDeptTree("t", "d");
        eq("雪花部门 id 原样", snow, String.valueOf(snowTree.get(0).getId()));
        eq("雪花人员 id 原样", snow, String.valueOf(snowTree.get(0).getStaffInfos().get(0).getId()));

        // ── 9. 同名不同码的两级（不按 name 匹配）─────────────────────
        List<DeptTreeVO> sameName = svc(depts(
                d(1L, "c1", "同名", null),
                d(2L, "c2", "同名", "c1")), emptyStaff()).buildDeptTree("t", "d");
        eq("同名按 deptCode 正确挂接", 1, sameName.get(0).getChildrenList().size());
        eq("同名子节点是 c2", "c2", sameName.get(0).getChildrenList().get(0).getDeptCode());

        System.out.println("\n----------------------------------------");
        System.out.println("AcDeptTreeService selftest: pass=" + pass + " fail=" + fail);
        System.out.println("----------------------------------------");
        if (fail > 0) {
            System.exit(1);
        }
    }

    // ── 内存桩 ──────────────────────────────────────────────────────

    /**
     * 构造被测服务。
     *
     * @param depts  部门数据
     * @param staffByDept 部门编码 → 该部门直属人员（显式映射，不靠推导）
     */
    private static AcDeptTreeServiceImpl svc(List<DeptDO> depts, Map<String, List<UserDO>> staffByDept) {
        // 拍平所有人员，供 UserDbService 按 id 返回
        Map<Long, UserDO> userById = new HashMap<>();
        for (List<UserDO> us : staffByDept.values()) {
            for (UserDO u : us) {
                userById.put(u.getId(), u);
            }
        }
        // 由映射反推 membership
        List<MembershipDO> mms = new ArrayList<>();
        for (Map.Entry<String, List<UserDO>> e : staffByDept.entrySet()) {
            for (UserDO u : e.getValue()) {
                MembershipDO m = new MembershipDO();
                m.setId((long) mms.size() + 1);
                m.setUserId(u.getId());
                m.setTenantCode("t");
                m.setDeptCode(e.getKey());
                m.setMemberStatus(1);
                mms.add(m);
            }
        }

        return new AcDeptTreeServiceImpl(
                new DeptDbService() {
                    @Override public int insert(DeptDO e) { throw new UnsupportedOperationException(); }
                    @Override public int updateById(DeptDO e) { throw new UnsupportedOperationException(); }
                    @Override public int deleteById(Long i) { throw new UnsupportedOperationException(); }
                    @Override public DeptDO selectByDeptCode(String a, String b, String c) { return null; }
                    @Override public List<DeptDO> selectByOrgCode(String a, String b, String c) { return Collections.emptyList(); }
                    @Override public List<DeptDO> selectByQuery(QueryWrapper<DeptDO> w) { return depts; }
                },
                new MembershipDbService() {
                    @Override public int insert(MembershipDO e) { throw new UnsupportedOperationException(); }
                    @Override public int updateById(MembershipDO e) { throw new UnsupportedOperationException(); }
                    @Override public int deleteById(Long i) { throw new UnsupportedOperationException(); }
                    @Override public MembershipDO selectById(Long i) { return null; }
                    @Override public MembershipDO selectByUserAndTenant(Long u, String t) { return null; }
                    @Override public List<MembershipDO> selectActiveByUserId(Long u) { return Collections.emptyList(); }
                    @Override public List<MembershipDO> selectAllByUserId(Long u) { return Collections.emptyList(); }
                    @Override public List<MembershipDO> selectActiveByTenant(String t) { return mms; }
                    @Override public long countActiveByUserId(Long u) { return 0; }
                    @Override public List<MembershipDO> selectByQuery(QueryWrapper<MembershipDO> w) { return mms; }
                },
                new UserDbService() {
                    @Override public int insert(UserDO e) { throw new UnsupportedOperationException(); }
                    @Override public int updateById(UserDO e) { throw new UnsupportedOperationException(); }
                    @Override public int deleteById(Long i) { throw new UnsupportedOperationException(); }
                    @Override public UserDO selectById(Long i) { return userById.get(i); }
                    @Override public UserDO selectByUserNo(String n) { return null; }
                    @Override public UserDO selectByEmail(String e) { return null; }
                    @Override public UserDO selectByPhone(String p) { return null; }
                    @Override public UserDO selectByUsername(String u) { return null; }
                    @Override public List<UserDO> selectByQuery(QueryWrapper<UserDO> w) {
                        return new ArrayList<>(userById.values());
                    }
                });
    }

    /** 无人员。 */
    private static Map<String, List<UserDO>> emptyStaff() {
        return Collections.emptyMap();
    }

    /** 显式构造 部门编码 → 人员列表 映射。 */
    private static Map<String, List<UserDO>> staffOf(Object... deptUserPairs) {
        Map<String, List<UserDO>> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < deptUserPairs.length; i += 2) {
            String deptCode = (String) deptUserPairs[i];
            m.put(deptCode, (List<UserDO>) deptUserPairs[i + 1]);
        }
        return m;
    }

    private static List<UserDO> users(Object... userIds) {
        List<UserDO> l = new ArrayList<>();
        for (Object o : userIds) {
            long id = ((Number) o).longValue();
            UserDO u = new UserDO();
            u.setId(id);
            u.setUserNo("U" + id);
            u.setUsername("u" + id);
            u.setNickname("u" + id);
            l.add(u);
        }
        return l;
    }

    private static DeptDO d(Long id, String code, String name, String parentCode) {
        DeptDO d = new DeptDO();
        d.setId(id);
        d.setDeptCode(code);
        d.setDeptName(name);
        d.setParentCode(parentCode);
        d.setOrgCode("org");
        d.setStatus(1);
        d.setTenantCode("t");
        d.setDomainCode("d");
        return d;
    }

    private static List<DeptDO> depts(DeptDO... ds) {
        List<DeptDO> l = new ArrayList<>();
        Collections.addAll(l, ds);
        return l;
    }

    private static List<DeptTreeVO> flatten(List<DeptTreeVO> nodes) {
        List<DeptTreeVO> out = new ArrayList<>();
        for (DeptTreeVO n : nodes) {
            out.add(n);
            out.addAll(flatten(n.getChildrenList()));
        }
        return out;
    }

    private static java.util.TreeSet<String> codes(List<DeptTreeVO> nodes) {
        return flatten(nodes).stream()
                .map(DeptTreeVO::getDeptCode)
                .collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    private static void eq(String name, Object expected, Object actual) {
        if (java.util.Objects.equals(expected, actual)) {
            pass++;
        } else {
            fail++;
            System.out.println("FAIL  " + name + "  expected=" + expected + " actual=" + actual);
        }
    }

    // 未使用但保留以防接口演进时需要
    @SuppressWarnings("unused")
    private static Map<Long, UserDO> noop() { return new HashMap<>(); }
}
