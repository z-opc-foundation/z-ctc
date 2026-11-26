package com.zifang.ctc.web.api;

import com.zifang.ctc.core.domain.entity.DeptDO;
import com.zifang.ctc.core.domain.entity.GroupDO;
import com.zifang.ctc.core.domain.entity.OrgDO;
import com.zifang.ctc.core.service.DeptService;
import com.zifang.ctc.core.service.GroupService;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.core.service.OrgService;
import com.zifang.ctc.core.service.UserService;
import com.zifang.ctc.core.vo.DeptVO;
import com.zifang.ctc.core.vo.GroupVO;
import com.zifang.ctc.core.vo.MemberVO;
import com.zifang.ctc.core.vo.OrgVO;
import com.zifang.util.core.lang.collection.Maps;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组织架构 (Org / Dept / Group) CRUD Controller.
 * <p>
 * API 基础路径: /api/ctc/ac
 * 所属模块: z-ctc-ac
 * 鉴权: 由 SsoInterceptor 统一拦截, 修改类操作通过 X-User-Id 头识别操作者
 *
 * <p>层级关系: Org (组织) → Dept (部门) → Group (小组).
 * 资源定位统一使用 (tenantCode, domainCode, code) 三元组, 其中:
 * <ul>
 *   <li>Org 通过 (tenantCode, domainCode, orgCode) 定位</li>
 *   <li>Dept 通过 (tenantCode, domainCode, deptCode) 定位, 隶属 orgCode</li>
 *   <li>Group 通过 (tenantCode, domainCode, groupCode) 定位, 隶属 deptCode</li>
 * </ul>
 *
 * <p>主要端点 (按资源):
 * <ul>
 *   <li>POST/GET/PUT/DELETE /api/ctc/ac/orgs[?orgCode=&tenantCode=&domainCode=] — 组织 CRUD (请求参数)</li>
 *   <li>POST/GET/PUT/DELETE /api/ctc/ac/depts[?deptCode=&tenantCode=&domainCode=&orgCode=] — 部门 CRUD (请求参数)</li>
 *   <li>POST/GET/PUT/DELETE /api/ctc/ac/groups[?groupCode=&tenantCode=&domainCode=&deptCode=] — 小组 CRUD (请求参数)</li>
 *   <li>GET /api/ctc/ac/depts/members[?tenantCode=&domainCode=&orgCode=|deptCode=] — 节点成员列表 (deptCode 优先, orgCode 展开为部门集合)</li>
 *   <li>POST/DELETE /api/ctc/ac/depts/members?tenantCode=&domainCode=&deptCode=&userId=[&roleCode=] — 挂载/摘除部门成员</li>
 *   <li>GET /api/ctc/ac/orgs/health — 模块健康检查</li>
 * </ul>
 *
 * <p>设计说明/关键约束:
 * <ul>
 *   <li>bean 名为 acOrgController, 避免与其他模块同名 Controller 冲突</li>
 *   <li>三个 create 接口对业务异常 (如编码冲突) 返回 409, 参数错误返回 400</li>
 *   <li>查询接口使用 (tenantCode, domainCode) 作为必填参数, 显式限定多租户域范围</li>
 * </ul>
 */
@RestController("acOrgController")
@RequestMapping("/api/ctc/ac")
public class OrgController {

    private final OrgService orgService;
    private final DeptService deptService;
    private final GroupService groupService;
    private final MembershipService membershipService;
    private final UserService userService;

    public OrgController(OrgService orgService, DeptService deptService, GroupService groupService,
                         MembershipService membershipService, UserService userService) {
        this.orgService = orgService;
        this.deptService = deptService;
        this.groupService = groupService;
        this.membershipService = membershipService;
        this.userService = userService;
    }

    // ===== Org =====

    /**
     * 新建组织 (Org). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400.
     *
     * @param org    组织实体
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回新建 Org 的主键 id 
     */
    @PostMapping("/orgs")
    @BizApi(summary = "新建组织 (Org). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400", description = "新建组织 (Org). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400.", tags = { "z-ctc" })
    public Result<Long> createOrg(@RequestBody @BizParam(name = "org", type = BizField.FieldType.STRING, description = "组织实体", position = BizParam.Position.BODY) OrgDO org,
                                  @RequestHeader(value = "X-User-Id", required = false)
                                  @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作者账号 id, 来自请求头 X-User-Id, 可选", position = BizParam.Position.HEADER) String userId) {
        try {
            Long id = orgService.createOrg(org, userId);
            return Result.<Long>success(id);
        } catch (IllegalStateException e) {
            return Result.<Long>fail(e.getMessage()).code(409);
        } catch (IllegalArgumentException e) {
            return Result.<Long>fail(e.getMessage()).code(400);
        }
    }

    /**
     * 列出指定 (租户, 域) 下的所有组织. tenantCode 与 domainCode 均为必填.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @return 该域下的 Org VO 列表
     */
    @GetMapping("/orgs/list")
    @BizApi(summary = "列出指定 (租户, 域) 下的所有组织. tenantCode 与 domainCode 均为必填", description = "列出指定 (租户, 域) 下的所有组织. tenantCode 与 domainCode 均为必填.", tags = { "z-ctc" })
    public Result<List<OrgVO>> listOrgs(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                        @RequestParam("domainCode")
                                        @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode) {
        return Result.success(orgService.listByDomain(tenantCode, domainCode).stream()
                .map(OrgVO::from)
                .collect(Collectors.toList()));
    }

    /**
     * 按编码查询组织详情. 找不到时返回 404.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param orgCode    组织编码, 请求参数 (?orgCode=xxx)
     * @return Org VO; 不存在时返回 404
     */
    @GetMapping("/orgs")
    @BizApi(summary = "按编码查询组织详情. 找不到时返回 404", description = "按编码查询组织详情. 找不到时返回 404.", tags = { "z-ctc" })
    public Result<OrgVO> getOrg(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                @RequestParam("domainCode")
                                @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                @RequestParam("orgCode")
                                @BizParam(name = "orgCode", type = BizField.FieldType.STRING, description = "组织编码, 请求参数 (?orgCode=xxx)", position = BizParam.Position.QUERY) String orgCode) {
        return orgService.findByCode(tenantCode, domainCode, orgCode)
                .map(o -> Result.<OrgVO>success(OrgVO.from(o)))
                .orElseGet(() -> Result.<OrgVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode()));
    }

    /**
     * 按编码更新组织字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param orgCode    组织编码, 请求参数 (?orgCode=xxx)
     * @param patch      待更新字段 (OrgDO)
     * @param userId     操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PutMapping("/orgs")
    @BizApi(summary = "按编码更新组织字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计", description = "按编码更新组织字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.", tags = { "z-ctc" })
    public Result<Void> updateOrg(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                  @RequestParam("domainCode")
                                  @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                  @RequestParam("orgCode")
                                  @BizParam(name = "orgCode", type = BizField.FieldType.STRING, description = "组织编码, 请求参数 (?orgCode=xxx)", position = BizParam.Position.QUERY) String orgCode,
                                  @RequestBody @BizParam(name = "patch", type = BizField.FieldType.STRING, description = "待更新字段 (OrgDO)", position = BizParam.Position.BODY) OrgDO patch,
                                  @RequestHeader(value = "X-User-Id", required = false)
                                  @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作者账号 id, 来自请求头 X-User-Id, 可选", position = BizParam.Position.HEADER) String userId) {
        boolean ok = orgService.updateOrg(tenantCode, domainCode, orgCode, patch, userId);
        return ok ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 按编码删除组织. 资源不存在时返回 404.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param orgCode    组织编码, 请求参数 (?orgCode=xxx)
     * @return 成功返回空体, 资源不存在时返回 404
     */
    @DeleteMapping("/orgs")
    @BizApi(summary = "按编码删除组织. 资源不存在时返回 404", description = "按编码删除组织. 资源不存在时返回 404.", tags = { "z-ctc" })
    public Result<Void> deleteOrg(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                  @RequestParam("domainCode")
                                  @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                  @RequestParam("orgCode")
                                  @BizParam(name = "orgCode", type = BizField.FieldType.STRING, description = "组织编码, 请求参数 (?orgCode=xxx)", position = BizParam.Position.QUERY) String orgCode) {
        boolean ok = orgService.deleteOrg(tenantCode, domainCode, orgCode);
        return ok ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    // ===== Dept =====

    /**
     * 新建部门 (Dept). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400.
     *
     * @param dept   部门实体
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回新建 Dept 的主键 id 
     */
    @PostMapping("/depts")
    @BizApi(summary = "新建部门 (Dept). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400", description = "新建部门 (Dept). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400.", tags = { "z-ctc" })
    public Result<Long> createDept(@RequestBody @BizParam(name = "dept", type = BizField.FieldType.STRING, description = "部门实体", position = BizParam.Position.BODY) DeptDO dept,
                                   @RequestHeader(value = "X-User-Id", required = false)
                                   @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作者账号 id, 来自请求头 X-User-Id, 可选", position = BizParam.Position.HEADER) String userId) {
        try {
            Long id = deptService.createDept(dept, userId);
            return Result.<Long>success(id);
        } catch (IllegalStateException e) {
            return Result.<Long>fail(e.getMessage()).code(409);
        } catch (IllegalArgumentException e) {
            return Result.<Long>fail(e.getMessage()).code(400);
        }
    }

    /**
     * 列出指定组织下的所有部门.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param orgCode    组织编码, 必填
     * @return 该组织下的 Dept VO 列表
     */
    @GetMapping("/depts/list")
    @BizApi(summary = "列出指定组织下的所有部门", description = "列出指定组织下的所有部门.", tags = { "z-ctc" })
    public Result<List<DeptVO>> listDepts(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                          @RequestParam("domainCode")
                                          @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                          @RequestParam("orgCode")
                                          @BizParam(name = "orgCode", type = BizField.FieldType.STRING, description = "组织编码, 必填", position = BizParam.Position.QUERY) String orgCode) {
        return Result.success(deptService.listByOrg(tenantCode, domainCode, orgCode).stream()
                .map(DeptVO::from)
                .collect(Collectors.toList()));
    }

    /**
     * 按编码查询部门详情. 找不到时返回 404.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param orgCode    隶属组织编码, 必填
     * @param deptCode   部门编码, 请求参数 (?deptCode=xxx)
     * @return Dept VO; 不存在时返回 404
     */
    @GetMapping("/depts")
    @BizApi(summary = "按编码查询部门详情. 找不到时返回 404", description = "按编码查询部门详情. 找不到时返回 404.", tags = { "z-ctc" })
    public Result<DeptVO> getDept(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                  @RequestParam("domainCode")
                                  @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                  @RequestParam("orgCode")
                                  @BizParam(name = "orgCode", type = BizField.FieldType.STRING, description = "隶属组织编码, 必填", position = BizParam.Position.QUERY) String orgCode,
                                  @RequestParam("deptCode")
                                  @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "部门编码, 请求参数 (?deptCode=xxx)", position = BizParam.Position.QUERY) String deptCode) {
        return deptService.findByCode(tenantCode, domainCode, deptCode)
                .map(d -> Result.<DeptVO>success(DeptVO.from(d)))
                .orElseGet(() -> Result.<DeptVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode()));
    }

    /**
     * 按编码更新部门字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param deptCode   部门编码, 请求参数 (?deptCode=xxx)
     * @param patch      待更新字段 (DeptDO)
     * @param userId     操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PutMapping("/depts")
    @BizApi(summary = "按编码更新部门字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计", description = "按编码更新部门字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.", tags = { "z-ctc" })
    public Result<Void> updateDept(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                   @RequestParam("domainCode")
                                   @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                   @RequestParam("deptCode")
                                   @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "部门编码, 请求参数 (?deptCode=xxx)", position = BizParam.Position.QUERY) String deptCode,
                                   @RequestBody @BizParam(name = "patch", type = BizField.FieldType.STRING, description = "待更新字段 (DeptDO)", position = BizParam.Position.BODY) DeptDO patch,
                                   @RequestHeader(value = "X-User-Id", required = false)
                                   @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作者账号 id, 来自请求头 X-User-Id, 可选", position = BizParam.Position.HEADER) String userId) {
        boolean ok = deptService.updateDept(tenantCode, domainCode, deptCode, patch, userId);
        return ok ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 按编码删除部门. 资源不存在返回 404.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param orgCode    隶属组织编码, 必填
     * @param deptCode   部门编码, 请求参数 (?deptCode=xxx)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @DeleteMapping("/depts")
    @BizApi(summary = "按编码删除部门. 资源不存在返回 404", description = "按编码删除部门. 资源不存在返回 404.", tags = { "z-ctc" })
    public Result<Void> deleteDept(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                   @RequestParam("domainCode")
                                   @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                   @RequestParam("orgCode")
                                   @BizParam(name = "orgCode", type = BizField.FieldType.STRING, description = "隶属组织编码, 必填", position = BizParam.Position.QUERY) String orgCode,
                                   @RequestParam("deptCode")
                                   @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "部门编码, 请求参数 (?deptCode=xxx)", position = BizParam.Position.QUERY) String deptCode) {
        boolean ok = deptService.deleteDept(tenantCode, domainCode, deptCode);
        return ok ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 列出挂在一个节点之下的成员 (组织树右面板).
     * <p>
     * 传 deptCode → 该部门的成员; 传 orgCode → 该组织下全部部门的成员并集.
     * membership 模型只有 dept 维度 (z_ctc_ac_membership.dept_code), org 节点由此展开.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param orgCode    组织编码, 与 deptCode 二选一
     * @param deptCode   部门编码, 优先于 orgCode
     * @return 成员 VO 列表 (含 username/nickname 富化)
     */
    @GetMapping("/depts/members")
    @BizApi(summary = "列出挂在一个节点之下的成员 (组织树右面板)", description = "列出挂在一个节点之下的成员 (组织树右面板). 传 deptCode → 该部门的成员; 传 orgCode → 该组织下全部部门的成员并集. membership 模型只有 dept 维度 (z_ctc_ac_membership.dept_code), org 节点由此展开.", tags = { "z-ctc" })
    public Result<List<MemberVO>> listDeptMembers(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                                  @RequestParam("domainCode")
                                                  @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                                  @RequestParam(value = "orgCode", required = false)
                                                  @BizParam(name = "orgCode", type = BizField.FieldType.STRING, description = "组织编码, 与 deptCode 二选一", position = BizParam.Position.QUERY) String orgCode,
                                                  @RequestParam(value = "deptCode", required = false)
                                                  @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "部门编码, 优先于 orgCode", position = BizParam.Position.QUERY) String deptCode) {
        List<com.zifang.ctc.core.domain.entity.MembershipDO> memberships;
        if (deptCode != null && !deptCode.isEmpty()) {
            memberships = membershipService.listActiveByDept(tenantCode, deptCode);
        } else if (orgCode != null && !orgCode.isEmpty()) {
            List<String> deptCodes = deptService.listByOrg(tenantCode, domainCode, orgCode).stream()
                    .map(DeptDO::getDeptCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            memberships = membershipService.listActiveByDeptCodes(tenantCode, deptCodes);
        } else {
            return Result.<List<MemberVO>>fail("orgCode / deptCode 必传其一").code(400);
        }

        List<Long> userIds = memberships.stream()
                .map(com.zifang.ctc.core.domain.entity.MembershipDO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, com.zifang.ctc.core.domain.entity.UserDO> userMap = userIds.isEmpty()
                ? Map.of()
                : userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(
                                com.zifang.ctc.core.domain.entity.UserDO::getId,
                                Function.identity(),
                                (a, b) -> a));

        return Result.success(memberships.stream()
                .map(m -> MemberVO.from(m, m.getUserId() != null ? userMap.get(m.getUserId()) : null))
                .collect(Collectors.toList()));
    }

    /**
     * 把账户挂到部门 (组织树成员卡片的「添加成员」).
     * uk_user_tenant 约束下一人一租户只有一条成员关系: 已有关系则改挂部门并回正状态.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param deptCode   部门编码, 必填
     * @param userId     账号主键 id, 必填
     * @param roleCode   角色编码, 可选 (默认 member)
     * @return 挂载后的成员 VO
     */
    @PostMapping("/depts/members")
    @BizApi(summary = "把账户挂到部门 (组织树成员卡片的「添加成员」)", description = "把账户挂到部门 (组织树成员卡片的「添加成员」). uk_user_tenant 约束下一人一租户只有一条成员关系: 已有关系则改挂部门并回正状态.", tags = { "z-ctc" })
    public Result<MemberVO> attachDeptMember(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                             @RequestParam("domainCode")
                                             @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                             @RequestParam("deptCode")
                                             @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "部门编码, 必填", position = BizParam.Position.QUERY) String deptCode,
                                             @RequestParam("userId")
                                             @BizParam(name = "userId", type = BizField.FieldType.INTEGER, description = "账号主键 id, 必填", position = BizParam.Position.QUERY) Long userId,
                                             @RequestParam(value = "roleCode", required = false)
                                             @BizParam(name = "roleCode", type = BizField.FieldType.STRING, description = "角色编码, 可选 (默认 member)", position = BizParam.Position.QUERY) String roleCode) {
        try {
            com.zifang.ctc.core.domain.entity.MembershipDO ms =
                    membershipService.attachToDept(userId, tenantCode, deptCode, roleCode);
            com.zifang.ctc.core.domain.entity.UserDO u =
                    userService.findById(userId).orElse(null);
            return Result.success(MemberVO.from(ms, u));
        } catch (IllegalArgumentException e) {
            return Result.<MemberVO>fail(e.getMessage()).code(400);
        }
    }

    /**
     * 把账户从部门摘下 — membership 保留, 仅清空 dept_code.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param deptCode   部门编码, 必填 (当前展示用的节点, 服务端按 user+tenant 定位)
     * @param userId     账号主键 id, 必填
     * @return 成功返回空体, 成员关系不存在返回 404
     */
    @DeleteMapping("/depts/members")
    @BizApi(summary = "把账户从部门摘下 — membership 保留, 仅清空 dept_code", description = "把账户从部门摘下 — membership 保留, 仅清空 dept_code.", tags = { "z-ctc" })
    public Result<Void> detachDeptMember(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                         @RequestParam("domainCode")
                                         @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                         @RequestParam("deptCode")
                                         @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "部门编码, 必填 (当前展示用的节点, 服务端按 user+tenant 定位)", position = BizParam.Position.QUERY) String deptCode,
                                         @RequestParam("userId")
                                         @BizParam(name = "userId", type = BizField.FieldType.INTEGER, description = "账号主键 id, 必填", position = BizParam.Position.QUERY) Long userId) {
        boolean ok = membershipService.detachFromDept(userId, tenantCode);
        return ok ? Result.<Void>success()
                : Result.<Void>fail("成员关系不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    // ===== Group =====

    /**
     * 新建小组 (Group). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400.
     *
     * @param group  小组实体
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回新建 Group 的主键 id 
     */
    @PostMapping("/groups")
    @BizApi(summary = "新建小组 (Group). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400", description = "新建小组 (Group). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400.", tags = { "z-ctc" })
    public Result<Long> createGroup(@RequestBody @BizParam(name = "group", type = BizField.FieldType.STRING, description = "小组实体", position = BizParam.Position.BODY) GroupDO group,
                                    @RequestHeader(value = "X-User-Id", required = false)
                                    @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作者账号 id, 来自请求头 X-User-Id, 可选", position = BizParam.Position.HEADER) String userId) {
        try {
            Long id = groupService.createGroup(group, userId);
            return Result.<Long>success(id);
        } catch (IllegalStateException e) {
            return Result.<Long>fail(e.getMessage()).code(409);
        } catch (IllegalArgumentException e) {
            return Result.<Long>fail(e.getMessage()).code(400);
        }
    }

    /**
     * 列出指定部门下的所有小组.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param deptCode   隶属部门编码, 必填
     * @return 该部门下的 Group VO 列表
     */
    @GetMapping("/groups/list")
    @BizApi(summary = "列出指定部门下的所有小组", description = "列出指定部门下的所有小组.", tags = { "z-ctc" })
    public Result<List<GroupVO>> listGroups(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                            @RequestParam("domainCode")
                                            @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                            @RequestParam("deptCode")
                                            @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "隶属部门编码, 必填", position = BizParam.Position.QUERY) String deptCode) {
        return Result.success(groupService.listByDept(tenantCode, domainCode, deptCode).stream()
                .map(GroupVO::from)
                .collect(Collectors.toList()));
    }

    /**
     * 按编码查询小组详情. 找不到时返回 404.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param deptCode   隶属部门编码, 必填
     * @param groupCode  小组编码, 请求参数 (?groupCode=xxx)
     * @return Group VO; 不存在时返回 404
     */
    @GetMapping("/groups")
    @BizApi(summary = "按编码查询小组详情. 找不到时返回 404", description = "按编码查询小组详情. 找不到时返回 404.", tags = { "z-ctc" })
    public Result<GroupVO> getGroup(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                    @RequestParam("domainCode")
                                    @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                    @RequestParam("deptCode")
                                    @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "隶属部门编码, 必填", position = BizParam.Position.QUERY) String deptCode,
                                    @RequestParam("groupCode")
                                    @BizParam(name = "groupCode", type = BizField.FieldType.STRING, description = "小组编码, 请求参数 (?groupCode=xxx)", position = BizParam.Position.QUERY) String groupCode) {
        return groupService.findByCode(tenantCode, domainCode, groupCode)
                .map(g -> Result.<GroupVO>success(GroupVO.from(g)))
                .orElseGet(() -> Result.<GroupVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode()));
    }

    /**
     * 按编码更新小组字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param deptCode   隶属部门编码, 必填
     * @param groupCode  小组编码, 请求参数 (?groupCode=xxx)
     * @param patch      待更新字段 (GroupDO)
     * @param userId     操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PutMapping("/groups")
    @BizApi(summary = "按编码更新小组字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计", description = "按编码更新小组字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.", tags = { "z-ctc" })
    public Result<Void> updateGroup(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                    @RequestParam("domainCode")
                                    @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                    @RequestParam("deptCode")
                                    @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "隶属部门编码, 必填", position = BizParam.Position.QUERY) String deptCode,
                                    @RequestParam("groupCode")
                                    @BizParam(name = "groupCode", type = BizField.FieldType.STRING, description = "小组编码, 请求参数 (?groupCode=xxx)", position = BizParam.Position.QUERY) String groupCode,
                                    @RequestBody @BizParam(name = "patch", type = BizField.FieldType.STRING, description = "待更新字段 (GroupDO)", position = BizParam.Position.BODY) GroupDO patch,
                                    @RequestHeader(value = "X-User-Id", required = false)
                                    @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作者账号 id, 来自请求头 X-User-Id, 可选", position = BizParam.Position.HEADER) String userId) {
        boolean ok = groupService.updateGroup(tenantCode, domainCode, groupCode, patch, userId);
        return ok ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 按编码删除小组. 资源不存在返回 404.
     *
     * @param tenantCode 租户编码, 必填
     * @param domainCode 域编码, 必填
     * @param deptCode   隶属部门编码, 必填
     * @param groupCode  小组编码, 请求参数 (?groupCode=xxx)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @DeleteMapping("/groups")
    @BizApi(summary = "按编码删除小组. 资源不存在返回 404", description = "按编码删除小组. 资源不存在返回 404.", tags = { "z-ctc" })
    public Result<Void> deleteGroup(@RequestParam("tenantCode") @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码, 必填", position = BizParam.Position.QUERY) String tenantCode,
                                    @RequestParam("domainCode")
                                    @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码, 必填", position = BizParam.Position.QUERY) String domainCode,
                                    @RequestParam("deptCode")
                                    @BizParam(name = "deptCode", type = BizField.FieldType.STRING, description = "隶属部门编码, 必填", position = BizParam.Position.QUERY) String deptCode,
                                    @RequestParam("groupCode")
                                    @BizParam(name = "groupCode", type = BizField.FieldType.STRING, description = "小组编码, 请求参数 (?groupCode=xxx)", position = BizParam.Position.QUERY) String groupCode) {
        boolean ok = groupService.deleteGroup(tenantCode, domainCode, groupCode);
        return ok ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 模块健康检查端点, 返回固定字段 (module / feature / status=ok). 用于探活/监控.
     *
     * @return 含 module、feature、status 三个键的成功结果
     */
    @GetMapping("/orgs/health")
    @BizApi(summary = "模块健康检查端点, 返回固定字段 (module / feature / status=ok). 用于探活/监控", description = "模块健康检查端点, 返回固定字段 (module / feature / status=ok). 用于探活/监控.", tags = { "z-ctc" })
    public Result<?> health() {
        return Result.success(Maps.of(
                "module", "z-ctc-ac",
                "feature", "org-dept-group",
                "status", "ok"
        ));
    }
}



