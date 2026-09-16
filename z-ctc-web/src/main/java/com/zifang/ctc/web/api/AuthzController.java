package com.zifang.ctc.web.api;

import com.zifang.ctc.common.vo.CheckPermissionVO;
import com.zifang.ctc.core.domain.entity.ResourceDO;
import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.service.AuthorizationService;
import com.zifang.ctc.core.vo.ResourceVO;
import com.zifang.ctc.core.vo.RoleVO;
import com.zifang.ctc.web.api.request.CheckRequest;
import com.zifang.ctc.web.api.request.GrantRoleResourceRequest;
import com.zifang.ctc.web.api.response.PageResourcesByAppResult;
import com.zifang.ctc.web.api.response.PageResourcesResult;
import com.zifang.ctc.web.api.response.PageRolesResult;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 授权 Controller (Authz = Authorization 简写, 与 02-modules.md 命名规约一致).
 * <p>
 * API 基础路径: /api/ctc/authz
 * 所属模块: z-ctc-authz
 * 鉴权: 由 SsoInterceptor 统一拦截, 修改类操作通过 X-User-Id 头识别操作者
 *
 * <p>核心数据模型:
 * <ul>
 *   <li>Role — 角色 (例如 "管理员", "访客")</li>
 *   <li>Resource — 资源/权限点 (例如 "user:create", "order:read"), 关联 appCode 区分应用</li>
 *   <li>User ↔ Role 多对多 (可设置过期时间)</li>
 *   <li>Role ↔ Resource 多对多 (可携带 dataFilter 做行级过滤)</li>
 * </ul>
 *
 * <p>端点:
 * <ul>
 *   <li>POST /api/ctc/authz/roles — 创建角色</li>
 *   <li>POST /api/ctc/authz/resources — 创建资源</li>
 *   <li>POST /api/ctc/authz/users/grant-role?userId=&roleId= — 授予用户角色 (请求参数)</li>
 *   <li>DELETE /api/ctc/authz/users/revoke-role?userId=&roleId= — 撤销 (请求参数)</li>
 *   <li>POST /api/ctc/authz/roles/grant-resource?roleId=&resourceId= — 角色授权 (请求参数)</li>
 *   <li>DELETE /api/ctc/authz/roles/revoke-resource?roleId=&resourceId= — 撤销 (请求参数)</li>
 *   <li>GET /api/ctc/authz/users/roles?userId= — 用户的角色 (请求参数)</li>
 *   <li>GET /api/ctc/authz/users/permissions?userId= — 用户的权限 (请求参数)</li>
 *   <li>POST /api/ctc/authz/check — 权限检查</li>
 *   <li>GET/PUT/DELETE /api/ctc/authz/roles[?id=] — Role CRUD (请求参数)</li>
 *   <li>GET/PUT/DELETE /api/ctc/authz/resources[?id=] — Resource CRUD (请求参数)</li>
 *   <li>GET /api/ctc/authz/resources/page-by-app — 按 appCode 分页查询资源 (FEATURE015)</li>
 *   <li>GET /api/ctc/authz/resources/distinct-apps — 已注册 appCode 列表</li>
 *   <li>GET /api/ctc/authz/resources/roles?resourceId= — 持有某资源的角色 (请求参数)</li>
 *   <li>GET /api/ctc/authz/roles/resources?roleId= — 某角色拥有的资源 (请求参数)</li>
 * </ul>
 *
 * <p>设计说明/关键约束:
 * <ul>
 *   <li>授权关系以 (user, role, expireAt) 和 (role, resource, dataFilter) 两种组合表达, grant/revoke 必须成对调用</li>
 *   <li>权限检查 (check) 仅做资源编码级的允许/拒绝判断, 不做行级 dataFilter 求值 (dataFilter 由业务方在数据访问层自行应用)</li>
 *   <li>所有写操作通过 X-User-Id 头识别授权人, 由 Service 层记录审计</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ctc/authz")
public class AuthzController {

    private final AuthorizationService authzService;

    public AuthzController(AuthorizationService authzService) {
        this.authzService = authzService;
    }

    /**
     * 创建角色.
     *
     * @param role   角色实体 (含 roleCode/roleName/描述)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回新建角色主键 id (HTTP 201)
     */
    @PostMapping("/roles")
    public Result<Long> createRole(@RequestBody RoleDO role,
                                   @RequestHeader(value = "X-User-Id", required = false) String userId) {
        Long id = authzService.createRole(role, userId);
        return Result.success(id).code(201);
    }

    /**
     * 创建资源/权限点. 资源应归属具体应用 (appCode), 后续按 appCode 维度管理.
     *
     * @param resource 资源实体 (含 resourceCode/resourceName/type/appCode)
     * @param userId   操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回新建资源主键 id (HTTP 201)
     */
    @PostMapping("/resources")
    public Result<Long> createResource(@RequestBody ResourceDO resource,
                                       @RequestHeader(value = "X-User-Id", required = false) String userId) {
        Long id = authzService.createResource(resource, userId);
        return Result.success(id).code(201);
    }

    /**
     * 授予用户某个角色. expireAt 为可选过期时间 (ISO-8601 字符串), 不传表示长期有效.
     *
     * @param userId    被授权的用户主键 id, 请求参数 (?userId=xxx)
     * @param roleId    角色主键 id, 请求参数 (?roleId=xxx)
     * @param expireAt  过期时间, ISO-8601 格式 (如 2026-12-31T23:59:59), 可选
     * @param grantedBy 授权人账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 参数错误返回 400
     */
    @PostMapping("/users/grant-role")
    public Result<Void> grantUserRole(
            @RequestParam("userId") Long userId,
            @RequestParam("roleId") Long roleId,
            @RequestParam(value = "expireAt", required = false) String expireAt,
            @RequestHeader(value = "X-User-Id", required = false) String grantedBy) {
        LocalDateTime exp = expireAt == null ? null : LocalDateTime.parse(expireAt);
        return authzService.grantUserRole(userId, roleId, grantedBy, exp)
                ? Result.<Void>success()
                : Result.<Void>fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
    }

    /**
     * 撤销用户的指定角色.
     *
     * @param userId 用户主键 id, 请求参数 (?userId=xxx)
     * @param roleId 角色主键 id, 请求参数 (?roleId=xxx)
     * @return 成功返回空体, 关系不存在返回 404
     */
    @DeleteMapping("/users/revoke-role")
    public Result<Void> revokeUserRole(@RequestParam("userId") Long userId,
                                         @RequestParam("roleId") Long roleId) {
        return authzService.revokeUserRole(userId, roleId)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 角色授权 — 把资源授予角色. 可选 dataFilter 用于行级数据过滤.
     *
     * @param roleId     角色主键 id, 请求参数 (?roleId=xxx)
     * @param resourceId 资源主键 id, 请求参数 (?resourceId=xxx)
     * @param body       含 dataFilter 的请求体, 可为 null (整体授权, 不做行级过滤)
     * @param grantedBy  授权人账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 参数错误返回 400
     */
    @PostMapping("/roles/grant-resource")
    public Result<Void> grantRoleResource(
            @RequestParam("roleId") Long roleId,
            @RequestParam("resourceId") Long resourceId,
            @RequestBody(required = false) GrantRoleResourceRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String grantedBy) {
        String dataFilter = body == null ? null : body.getDataFilter();
        return authzService.grantRoleResource(roleId, resourceId, dataFilter, grantedBy)
                ? Result.<Void>success()
                : Result.<Void>fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
    }

    /**
     * 撤销角色对资源的授权.
     *
     * @param roleId     角色主键 id, 请求参数 (?roleId=xxx)
     * @param resourceId 资源主键 id, 请求参数 (?resourceId=xxx)
     * @return 成功返回空体, 关系不存在返回 404
     */
    @DeleteMapping("/roles/revoke-resource")
    public Result<Void> revokeRoleResource(@RequestParam("roleId") Long roleId,
                                             @RequestParam("resourceId") Long resourceId) {
        return authzService.revokeRoleResource(roleId, resourceId)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 列出用户拥有的所有角色 (含未过期判断).
     *
     * @param userId 用户主键 id, 请求参数 (?userId=xxx)
     * @return 该用户当前有效的 Role VO 列表
     */
    @GetMapping("/users/roles")
    public Result<List<RoleVO>> listUserRoles(@RequestParam("userId") Long userId) {
        List<RoleVO> voList = authzService.listUserRoles(userId).stream()
                .map(RoleVO::from)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 列出用户通过角色间接拥有的所有资源/权限点 (展开后去重).
     *
     * @param userId 用户主键 id, 请求参数 (?userId=xxx)
     * @return 该用户可访问的 Resource VO 列表
     */
    @GetMapping("/users/permissions")
    public Result<List<ResourceVO>> listUserPermissions(@RequestParam("userId") Long userId) {
        List<ResourceVO> voList = authzService.listUserPermissions(userId).stream()
                .map(ResourceVO::from)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 权限检查: 给定 (userId, resourceCode) 判断是否允许访问. 资源编码级的二元判断, 不解析 dataFilter.
     *
     * @param request 含 userId 与 resourceCode 的请求体
     * @return CheckPermissionVO { userId, resourceCode, allowed }; 必填字段缺失返回 400
     */
    @PostMapping("/check")
    public Result<CheckPermissionVO> checkPermission(@RequestBody CheckRequest request) {
        if (request == null || request.getUserId() == null || request.getResourceCode() == null) {
            return (Result) Result.fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        boolean allowed = authzService.checkPermission(request.getUserId(), request.getResourceCode());
        CheckPermissionVO body = new CheckPermissionVO(request.getUserId(), request.getResourceCode(), allowed);
        return Result.success(body);
    }

    // ===== FEATURE014 补齐 Role CRUD =====

    /**
     * 全量角色列表, 不分页.
     *
     * @return 全量 Role VO 列表
     */
    @GetMapping("/roles/list")
    public Result<List<RoleVO>> listAllRoles() {
        List<RoleVO> voList = authzService.listAllRoles().stream()
                .map(RoleVO::from)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 角色分页查询: 按 roleCode/roleName 不区分大小写子串匹配, 返回当前页与过滤后的 total.
     *
     * @param keyword  关键字, 可选
     * @param pageNum  页码, 默认 1
     * @param pageSize 每页大小, 默认 20
     * @return 含 data 与 total 字段的 Map
     */
    @GetMapping("/roles/page")
    public Result<PageRolesResult> pageRoles(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        List<RoleDO> data = authzService.pageRoles(keyword, pageNum, pageSize, 0);
        long total = authzService.listAllRoles().stream()
                .filter(r -> {
                    if (keyword == null || keyword.isEmpty()) { return true; }
                    String k = keyword.toLowerCase();
                    return (r.getRoleCode() != null && r.getRoleCode().toLowerCase().contains(k))
                            || (r.getRoleName() != null && r.getRoleName().toLowerCase().contains(k));
                }).count();
        PageRolesResult body = new PageRolesResult(
                data.stream().map(RoleVO::from).collect(Collectors.toList()),
                total);
        return Result.success(body);
    }

    /**
     * 按主键查询角色详情. 不存在返回 404.
     *
     * @param id 角色主键 id, 请求参数 (?id=xxx)
     * @return Role VO; 不存在返回 404
     */
    @GetMapping("/roles")
    public Result<RoleVO> findRoleById(@RequestParam("id") Long id) {
        return authzService.findRoleById(id)
                .<Result<RoleVO>>map(t -> Result.success(RoleVO.from(t)))
                .orElse(Result.<RoleVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode()));
    }

    /**
     * 按主键更新角色字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.
     *
     * @param id     角色主键 id, 请求参数 (?id=xxx)
     * @param patch  待更新字段 (RoleDO)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PutMapping("/roles")
    public Result<Void> updateRole(
            @RequestParam("id") Long id,
            @RequestBody RoleDO patch,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return authzService.updateRole(id, patch, userId)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 按主键删除角色. 资源不存在返回 404.
     *
     * @param id 角色主键 id, 请求参数 (?id=xxx)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @DeleteMapping("/roles")
    public Result<Void> deleteRole(@RequestParam("id") Long id) {
        return authzService.deleteRole(id)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    // ===== FEATURE014 补齐 Resource CRUD =====

    /**
     * 全量资源列表, 不分页.
     *
     * @return 全量 Resource VO 列表
     */
    @GetMapping("/resources/list")
    public Result<List<ResourceVO>> listAllResources() {
        List<ResourceVO> voList = authzService.listAllResources().stream()
                .map(ResourceVO::from)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 资源分页查询: 按 resourceCode/resourceName 不区分大小写子串匹配, 返回当前页与过滤后的 total.
     *
     * @param keyword  关键字, 可选
     * @param pageNum  页码, 默认 1
     * @param pageSize 每页大小, 默认 20
     * @return 含 data 与 total 字段的 Map
     */
    @GetMapping("/resources/page")
    public Result<PageResourcesResult> pageResources(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        List<ResourceDO> data = authzService.pageResources(keyword, pageNum, pageSize, 0);
        long total = authzService.listAllResources().stream()
                .filter(r -> {
                    if (keyword == null || keyword.isEmpty()) { return true; }
                    String k = keyword.toLowerCase();
                    return (r.getResourceCode() != null && r.getResourceCode().toLowerCase().contains(k))
                            || (r.getResourceName() != null && r.getResourceName().toLowerCase().contains(k));
                }).count();
        PageResourcesResult body = new PageResourcesResult(
                data.stream().map(ResourceVO::from).collect(Collectors.toList()),
                total);
        return Result.success(body);
    }

    /**
     * 按主键查询资源详情. 不存在返回 404.
     *
     * @param id 资源主键 id, 请求参数 (?id=xxx)
     * @return Resource VO; 不存在返回 404
     */
    @GetMapping("/resources")
    public Result<ResourceVO> findResourceById(@RequestParam("id") Long id) {
        return authzService.findResourceById(id)
                .<Result<ResourceVO>>map(t -> Result.success(ResourceVO.from(t)))
                .orElse(Result.<ResourceVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode()));
    }

    /**
     * 按主键更新资源字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.
     *
     * @param id     资源主键 id, 请求参数 (?id=xxx)
     * @param patch  待更新字段 (ResourceDO)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PutMapping("/resources")
    public Result<Void> updateResource(
            @RequestParam("id") Long id,
            @RequestBody ResourceDO patch,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return authzService.updateResource(id, patch, userId)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 按主键删除资源. 资源不存在返回 404.
     *
     * @param id 资源主键 id, 请求参数 (?id=xxx)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @DeleteMapping("/resources")
    public Result<Void> deleteResource(@RequestParam("id") Long id) {
        return authzService.deleteResource(id)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    // ===== FEATURE015: 按应用维度管理权限点 =====

    /**
     * 按 appCode 分页查询资源. 用于 FEATURE015 资源按应用维度管理.
     *
     * @param appCode  应用编码, 可选; 为空时不过滤
     * @param type     资源类型, 可选; 为空时不过滤
     * @param keyword  关键字, 可选; 匹配 resourceCode/resourceName
     * @param pageNum  页码, 默认 1
     * @param pageSize 每页大小, 默认 20
     * @return 含 data 与 total 字段的 Map (total 用同条件全量在内存里计算)
     */
    @GetMapping("/resources/page-by-app")
    public Result<PageResourcesByAppResult> pageResourcesByApp(
            @RequestParam(value = "appCode", required = false) String appCode,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        List<ResourceDO> data = authzService.pageResourcesByApp(appCode, type, keyword, pageNum, pageSize, 0);
        // 用同条件在内存里数 total（量级预期 < 10k，可接受）
        List<ResourceDO> all = authzService.pageResourcesByApp(appCode, type, keyword, 1, Integer.MAX_VALUE, 0);
        long total = all.size();
        PageResourcesByAppResult body = new PageResourcesByAppResult(
                data.stream().map(ResourceVO::from).collect(Collectors.toList()),
                total);
        return Result.success(body);
    }

    /**
     * DISTINCT 出资源表中已注册的所有 appCode 列表. 用于前端应用下拉/筛选.
     *
     * @return 去重后的 appCode 字符串列表
     */
    @GetMapping("/resources/distinct-apps")
    public Result<List<String>> distinctApps() {
        return Result.success(authzService.listDistinctApps());
    }

    /**
     * 持有指定资源的角色列表 — 资源详情抽屉「关联角色」tab 用.
     *
     * @param resourceId 资源主键 id, 请求参数 (?resourceId=xxx)
     * @return 持有该资源的 Role VO 列表
     */
    @GetMapping("/resources/roles")
    public Result<List<RoleVO>> listRolesByResource(@RequestParam("resourceId") Long resourceId) {
        List<RoleVO> voList = authzService.listRolesByResource(resourceId).stream()
                .map(RoleVO::from)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 查询指定角色拥有的资源/权限点列表.
     *
     * @param roleId 角色主键 id, 请求参数 (?roleId=xxx)
     * @return 该角色关联的 Resource VO 列表
     */
    @GetMapping("/roles/resources")
    public Result<List<ResourceVO>> listRolePermissions(@RequestParam("roleId") Long roleId) {
        List<ResourceVO> voList = authzService.listRolePermissions(roleId).stream()
                .map(ResourceVO::from)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    // ===== RBAC+Override: 用户直接授权 (白名单, 加集) =====

    /**
     * 给用户直接授权一个资源 (白名单). 幂等: 已存在则跳过.
     *
     * @param userId     被授权用户 id, 请求参数
     * @param resourceId 资源 id, 请求参数
     * @param expireAt   过期时间, 可选
     * @param grantedBy  授权人, 来自 X-User-Id 头
     */
    @PostMapping("/users/grant-resource")
    public Result<Void> grantUserResource(
            @RequestParam("userId") Long userId,
            @RequestParam("resourceId") Long resourceId,
            @RequestParam(value = "expireAt", required = false) String expireAt,
            @RequestHeader(value = "X-User-Id", required = false) String grantedBy) {
        LocalDateTime exp = expireAt == null ? null : LocalDateTime.parse(expireAt);
        return authzService.grantUserResource(userId, resourceId, null, grantedBy, exp)
                ? Result.<Void>success()
                : Result.<Void>fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
    }

    /**
     * 撤销用户直接授权的一个资源.
     */
    @DeleteMapping("/users/revoke-resource")
    public Result<Void> revokeUserResource(
            @RequestParam("userId") Long userId,
            @RequestParam("resourceId") Long resourceId) {
        return authzService.revokeUserResource(userId, resourceId)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 查用户的直接授权列表 (白名单).
     */
    @GetMapping("/users/grants")
    public Result<?> listUserGrants(@RequestParam("userId") Long userId) {
        return Result.success(authzService.listUserGrants(userId));
    }

    // ===== RBAC+Override: 用户直接拒绝 (黑名单, 减集) =====

    /**
     * 给用户设置拒绝一个资源 (黑名单). 幂等: 已存在则跳过.
     */
    @PostMapping("/users/deny-resource")
    public Result<Void> denyUserResource(
            @RequestParam("userId") Long userId,
            @RequestParam("resourceId") Long resourceId,
            @RequestParam(value = "expireAt", required = false) String expireAt,
            @RequestHeader(value = "X-User-Id", required = false) String deniedBy) {
        LocalDateTime exp = expireAt == null ? null : LocalDateTime.parse(expireAt);
        return authzService.denyUserResource(userId, resourceId, null, deniedBy, exp)
                ? Result.<Void>success()
                : Result.<Void>fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
    }

    /**
     * 撤销用户对某资源的拒绝.
     */
    @DeleteMapping("/users/undeny-resource")
    public Result<Void> undenyUserResource(
            @RequestParam("userId") Long userId,
            @RequestParam("resourceId") Long resourceId) {
        return authzService.undenyUserResource(userId, resourceId)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 查用户的直接拒绝列表 (黑名单).
     */
    @GetMapping("/users/denies")
    public Result<?> listUserDenies(@RequestParam("userId") Long userId) {
        return Result.success(authzService.listUserDenies(userId));
    }
}
