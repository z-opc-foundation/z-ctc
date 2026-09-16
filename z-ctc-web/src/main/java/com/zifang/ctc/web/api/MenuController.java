package com.zifang.ctc.web.api;

import com.zifang.ctc.core.domain.entity.MenuDO;
import com.zifang.ctc.core.domain.entity.ResourceDO;
import com.zifang.ctc.core.service.AuthorizationService;
import com.zifang.ctc.core.service.MenuService;
import com.zifang.ctc.core.vo.MenuVO;
import com.zifang.ctc.web.api.response.MenuListResult;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用菜单 Controller.
 * <p>
 * API 基础路径: /api/ctc/app-menu
 * 所属模块: z-ctc-authz
 * 鉴权: 不强制鉴权 (沿用 z-ctc-authz 的开放风格), 列表/树端点允许未登录访问
 *
 * <p>端点:
 * <ul>
 *   <li>GET /api/ctc/app-menu/tree?appCode=z-opc — 拉菜单树 (前端 {@code getDynamicMenu} 用)</li>
 *   <li>GET /api/ctc/app-menu/list?appCode=z-opc — 扁平列表 (管理页)</li>
 *   <li>POST /api/ctc/app-menu — 创建</li>
 *   <li>PUT /api/ctc/app-menu?id=xxx — 更新 (请求参数)</li>
 *   <li>DELETE /api/ctc/app-menu?id=xxx — 删除 (级联, 请求参数)</li>
 *   <li>GET /api/ctc/app-menu?id=xxx — 详情 (请求参数)</li>
 * </ul>
 * <p>
 * 设计要点:
 * <ul>
 *   <li>不强制鉴权 (沿用 z-ctc-authz 的开放风格), 列表/树端点允许未登录访问 — App.jsx 启动时会先调用以决定菜单.</li>
 *   <li>写入操作读 X-User-Id 头, 写入 createdBy / updatedBy 字段 (留给后续审计/留痕).</li>
 *   <li>菜单树字段名采用 camelCase 序列化, 匹配前端 convertMenuItems 的访问约定.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ctc/app-menu")
public class MenuController {

    private final MenuService menuService;
    private final AuthorizationService authzService;

    public MenuController(MenuService menuService, AuthorizationService authzService) {
        this.menuService = menuService;
        this.authzService = authzService;
    }

    /**
     * 拉取指定 app 的菜单树 (核心端点, 给前端 {@code getDynamicMenu('z-opc')} 用).
     * <p>
     * 返回结构:
     * <pre>
     * [
     *   {
     *     "id": 1, "menuCode": "ctc", "menuName": "4A中心",
     *     "icon": "UserOutlined", "path": "/ctc",
     *     "sourceType": "CODE", "pageId": null, "component": null,
     *     "children": [ { ... }, ... ]
     *   },
     *   ...
     * ]
     * </pre>
     * 字段名采用 camelCase 序列化, 匹配前端 convertMenuItems 的访问.
     */
    @GetMapping("/tree")
    public Result<List<MenuVO>> getTree(
            @RequestParam(value = "appCode", required = false) String appCode) {
        if (appCode == null || appCode.isEmpty()) {
            return (Result) Result.fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        List<MenuDO> tree = menuService.listAsTree(appCode);
        List<MenuVO> voList = tree.stream().map(MenuVO::from).collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 扁平列表 — 菜单管理页用. 返回 { data, total } 结构 (非分页, 一次返回该 app 的全部菜单).
     *
     * @param appCode 应用编码, 必填
     * @return 含 data (扁平 MenuVO 列表) 与 total (列表长度) 的 Map
     */
    @GetMapping("/list")
    public Result<MenuListResult> list(
            @RequestParam(value = "appCode", required = false) String appCode) {
        if (appCode == null || appCode.isEmpty()) {
            return (Result) Result.fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        List<MenuDO> data = menuService.listFlat(appCode);
        MenuListResult body = new MenuListResult(
                data.stream().map(MenuVO::from).collect(Collectors.toList()),
                data.size());
        return Result.success(body);
    }

    /**
     * 按主键查询菜单详情. 不存在返回 404.
     *
     * @param id 菜单主键 id, 请求参数 (?id=xxx)
     * @return Menu VO; 不存在返回 404
     */
    @GetMapping
    public Result<MenuVO> getById(@RequestParam("id") Long id) {
        return menuService.findById(id)
                .<Result<MenuVO>>map(t -> Result.success(MenuVO.from(t)))
                .orElse(Result.<MenuVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode()));
    }

    /**
     * 创建菜单. 判重冲突返回 409, 参数错误返回 400. 操作者通过 X-User-Id 头传递.
     *
     * @param menu   菜单实体 (含 appCode/menuCode/menuName/path 等)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回新建菜单主键 id (HTTP 201)
     */
    @PostMapping
    public Result<Long> create(
            @RequestBody MenuDO menu,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Long id = menuService.createMenu(menu, userId);
            return Result.success(id).code(201);
        } catch (IllegalStateException e) {
            // 判重冲突 — 返回 409
            return (Result) Result.fail("error").code(HttpStatus.CONFLICT.value());
        } catch (IllegalArgumentException e) {
            return (Result) Result.fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
    }

    /**
     * 按主键更新菜单字段. 操作者通过 X-User-Id 头传递, 由 Service 层写入 updatedBy 审计字段.
     *
     * @param id     菜单主键 id, 请求参数 (?id=xxx)
     * @param patch  待更新字段 (MenuDO)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PutMapping
    public Result<Void> update(
            @RequestParam("id") Long id,
            @RequestBody MenuDO patch,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return menuService.updateMenu(id, patch, userId)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 按主键删除菜单 (级联删除子菜单). 资源不存在返回 404.
     *
     * @param id 菜单主键 id, 请求参数 (?id=xxx)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam("id") Long id) {
        return menuService.deleteMenu(id)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    // ===== RBAC+Override: 按用户权限过滤菜单树 =====

    /**
     * 按用户权限过滤的菜单树.
     * <p>
     * 逻辑:
     * <ol>
     *   <li>拉取 appCode 下的全量菜单树</li>
     *   <li>获取用户最终权限 (角色权限 ∪ grant - deny)</li>
     *   <li>过滤: resource_id=NULL 的菜单所有人可见; 非NULL的需要用户持有该 resource_id 的权限</li>
     *   <li>如果某菜单的子节点全部被过滤, 父节点也隐藏</li>
     * </ol>
     *
     * @param appCode 应用编码, 必填
     * @param userId  用户主键 id, 请求参数
     * @return 过滤后的菜单树
     */
    @GetMapping("/tree-by-user")
    public Result<List<MenuVO>> getTreeByUser(
            @RequestParam("appCode") String appCode,
            @RequestParam("userId") Long userId) {
        if (appCode == null || appCode.isEmpty() || userId == null) {
            return (Result) Result.fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        // 1. 拉全量菜单树
        List<MenuDO> fullTree = menuService.listAsTree(appCode);
        // 2. 获取用户权限 resource_id 集合
        List<ResourceDO> perms = authzService.listUserPermissions(userId);
        Set<Long> allowedResourceIds = perms.stream().map(ResourceDO::getId).collect(Collectors.toSet());
        // 3. 递归过滤
        List<MenuDO> filtered = filterMenuTree(fullTree, allowedResourceIds);
        List<MenuVO> voList = filtered.stream().map(MenuVO::from).collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 递归过滤菜单树: resource_id=NULL 放行, 非NULL 需在 allowedResourceIds 中.
     * 子节点全被过滤则父节点也隐藏.
     */
    private List<MenuDO> filterMenuTree(List<MenuDO> nodes, Set<Long> allowedResourceIds) {
        if (nodes == null) { return Collections.emptyList(); }

        List<MenuDO> result = new ArrayList<>();
        for (MenuDO node : nodes) {
            // 先过滤子节点
            List<MenuDO> filteredChildren = filterMenuTree(node.getChildren(), allowedResourceIds);
            // 判断本节点是否可见
            boolean visible = isMenuVisible(node, allowedResourceIds);
            if (visible) {
                node.setChildren(filteredChildren);
                result.add(node);
            } else if (!filteredChildren.isEmpty()) {
                // 本节点不可见但子节点有可见的 — 提升子节点
                result.addAll(filteredChildren);
            }
        }
        return result;
    }

    /**
     * 判断单个菜单是否对用户可见.
     * resource_id=NULL → 所有人可见; 非NULL → 需用户持有该权限.
     */
    private boolean isMenuVisible(MenuDO menu, Set<Long> allowedResourceIds) {
        if (menu.getResourceId() == null) return true; // 无权限控制
        return allowedResourceIds.contains(menu.getResourceId());
    }
}
