package com.zifang.ctc.web.api;

import com.zifang.ctc.core.domain.entity.DeptDO;
import com.zifang.ctc.core.domain.entity.GroupDO;
import com.zifang.ctc.core.domain.entity.OrgDO;
import com.zifang.ctc.core.service.DeptService;
import com.zifang.ctc.core.service.GroupService;
import com.zifang.ctc.core.service.OrgService;
import com.zifang.ctc.core.vo.DeptVO;
import com.zifang.ctc.core.vo.GroupVO;
import com.zifang.ctc.core.vo.OrgVO;
import com.zifang.util.core.lang.collection.Maps;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    public OrgController(OrgService orgService, DeptService deptService, GroupService groupService) {
        this.orgService = orgService;
        this.deptService = deptService;
        this.groupService = groupService;
    }

    // ===== Org =====

    /**
     * 新建组织 (Org). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400.
     *
     * @param org    组织实体
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回新建 Org 的主键 id (HTTP 201)
     */
    @PostMapping("/orgs")
    public Result<Long> createOrg(@RequestBody OrgDO org,
                                  @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Long id = orgService.createOrg(org, userId);
            return Result.<Long>success(id).code(201);
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
    public Result<List<OrgVO>> listOrgs(@RequestParam("tenantCode") String tenantCode,
                                        @RequestParam("domainCode") String domainCode) {
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
    public Result<OrgVO> getOrg(@RequestParam("tenantCode") String tenantCode,
                                @RequestParam("domainCode") String domainCode,
                                @RequestParam("orgCode") String orgCode) {
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
    public Result<Void> updateOrg(@RequestParam("tenantCode") String tenantCode,
                                  @RequestParam("domainCode") String domainCode,
                                  @RequestParam("orgCode") String orgCode,
                                  @RequestBody OrgDO patch,
                                  @RequestHeader(value = "X-User-Id", required = false) String userId) {
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
    public Result<Void> deleteOrg(@RequestParam("tenantCode") String tenantCode,
                                  @RequestParam("domainCode") String domainCode,
                                  @RequestParam("orgCode") String orgCode) {
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
     * @return 成功返回新建 Dept 的主键 id (HTTP 201)
     */
    @PostMapping("/depts")
    public Result<Long> createDept(@RequestBody DeptDO dept,
                                   @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Long id = deptService.createDept(dept, userId);
            return Result.<Long>success(id).code(201);
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
    public Result<List<DeptVO>> listDepts(@RequestParam("tenantCode") String tenantCode,
                                          @RequestParam("domainCode") String domainCode,
                                          @RequestParam("orgCode") String orgCode) {
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
    public Result<DeptVO> getDept(@RequestParam("tenantCode") String tenantCode,
                                  @RequestParam("domainCode") String domainCode,
                                  @RequestParam("orgCode") String orgCode,
                                  @RequestParam("deptCode") String deptCode) {
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
    public Result<Void> updateDept(@RequestParam("tenantCode") String tenantCode,
                                   @RequestParam("domainCode") String domainCode,
                                   @RequestParam("deptCode") String deptCode,
                                   @RequestBody DeptDO patch,
                                   @RequestHeader(value = "X-User-Id", required = false) String userId) {
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
    public Result<Void> deleteDept(@RequestParam("tenantCode") String tenantCode,
                                   @RequestParam("domainCode") String domainCode,
                                   @RequestParam("orgCode") String orgCode,
                                   @RequestParam("deptCode") String deptCode) {
        boolean ok = deptService.deleteDept(tenantCode, domainCode, deptCode);
        return ok ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    // ===== Group =====

    /**
     * 新建小组 (Group). 业务冲突 (如编码重复) 返回 409, 参数错误返回 400.
     *
     * @param group  小组实体
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回新建 Group 的主键 id (HTTP 201)
     */
    @PostMapping("/groups")
    public Result<Long> createGroup(@RequestBody GroupDO group,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Long id = groupService.createGroup(group, userId);
            return Result.<Long>success(id).code(201);
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
    public Result<List<GroupVO>> listGroups(@RequestParam("tenantCode") String tenantCode,
                                            @RequestParam("domainCode") String domainCode,
                                            @RequestParam("deptCode") String deptCode) {
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
    public Result<GroupVO> getGroup(@RequestParam("tenantCode") String tenantCode,
                                    @RequestParam("domainCode") String domainCode,
                                    @RequestParam("deptCode") String deptCode,
                                    @RequestParam("groupCode") String groupCode) {
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
    public Result<Void> updateGroup(@RequestParam("tenantCode") String tenantCode,
                                    @RequestParam("domainCode") String domainCode,
                                    @RequestParam("deptCode") String deptCode,
                                    @RequestParam("groupCode") String groupCode,
                                    @RequestBody GroupDO patch,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
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
    public Result<Void> deleteGroup(@RequestParam("tenantCode") String tenantCode,
                                    @RequestParam("domainCode") String domainCode,
                                    @RequestParam("deptCode") String deptCode,
                                    @RequestParam("groupCode") String groupCode) {
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
    public Result<?> health() {
        return Result.success(Maps.of(
                "module", "z-ctc-ac",
                "feature", "org-dept-group",
                "status", "ok"
        ));
    }
}
