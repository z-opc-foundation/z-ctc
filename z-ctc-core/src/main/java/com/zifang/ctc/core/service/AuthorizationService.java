package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.ResourceDO;
import com.zifang.ctc.core.domain.entity.RoleDO;

import java.util.List;
import java.util.Optional;

/**
 * 授权服务接口 — RBAC 核心 + 数据权限占位.
 * <p>
 * 设计哲学:
 * 4A 中心的 "A2 Authorization" 域 = "你能做什么" 这一步. z-ctc-authz 提供:
 * <ul>
 *   <li>角色 CRUD (RoleService)</li>
 *   <li>资源 CRUD (ResourceService)</li>
 *   <li>用户↔角色 授权 / 撤销 (UserRoleService)</li>
 *   <li>角色↔资源 授权 / 撤销 (RoleResourceService, 含 dataFilter 表达式)</li>
 *   <li>用户全部权限查询 (AuthorizationService.listUserPermissions)</li>
 *   <li>单点权限检查 (AuthorizationService.checkPermission)</li>
 * </ul>
 * <p>
 * 数据权限: resource_type=2 时, role_resource.data_filter 存 SpEL 表达式,
 * 业务方在数据查询入口用 SpEL 解析器执行 (避免引入新依赖).
 */
public interface AuthorizationService {

    Long createRole(RoleDO role, String createdBy);

    Long createResource(ResourceDO resource, String createdBy);

    boolean grantUserRole(Long userId, Long roleId, String grantedBy, java.time.LocalDateTime expireAt);

    boolean revokeUserRole(Long userId, Long roleId);

    boolean grantRoleResource(Long roleId, Long resourceId, String dataFilter, String grantedBy);

    boolean revokeRoleResource(Long roleId, Long resourceId);

    List<RoleDO> listUserRoles(Long userId);

    List<ResourceDO> listUserPermissions(Long userId);

    List<ResourceDO> listRolePermissions(Long roleId);

    boolean checkPermission(Long userId, String resourceCode);

    Optional<RoleDO> findRoleByCode(String roleCode, String tenantCode);

    /**
     * 按 resource_code + 可选 app_code + 可选 tenant_code 查资源.
     * <p>
     * 业务侧已知自己所属 app 时必须传 appCode, 避免跨 app 同名资源命中歧义 (FEATURE-ZTEAM-AUTH-UNIFY 段 4).
     *
     * @param resourceCode 资源编码
     * @param appCode      应用编码; null/空表示不过滤
     * @param tenantCode   租户编码; null 表示不过滤
     */
    Optional<ResourceDO> findResourceByCode(String resourceCode, String appCode, String tenantCode);

    /**
     * 旧签名委托三参版本 (appCode=null), 仅用于 admin 通用视图. 业务侧请改用三参版本.
     */
    Optional<ResourceDO> findResourceByCode(String resourceCode, String tenantCode);

    // FEATURE014: 补齐 CRUD

    /**
     * 角色全量列表
     */
    List<RoleDO> listAllRoles();

    /**
     * 角色分页
     */
    List<RoleDO> pageRoles(String keyword, int pageNum, int pageSize, long total);

    /**
     * 角色按 ID
     */
    Optional<RoleDO> findRoleById(Long id);

    /**
     * 角色更新 (非空字段)
     */
    boolean updateRole(Long id, RoleDO patch, String updatedBy);

    /**
     * 角色删除 (先解绑 user_role + role_resource)
     */
    boolean deleteRole(Long id);

    /**
     * 资源全量列表
     */
    List<ResourceDO> listAllResources();

    /**
     * 资源分页
     */
    List<ResourceDO> pageResources(String keyword, int pageNum, int pageSize, long total);

    /**
     * 资源按 ID
     */
    Optional<ResourceDO> findResourceById(Long id);

    /**
     * 资源更新
     */
    boolean updateResource(Long id, ResourceDO patch, String updatedBy);

    /**
     * 资源删除 (先解绑 role_resource)
     */
    boolean deleteResource(Long id);

    // FEATURE015: 按应用维度管理权限点

    /**
     * 按应用+类型+关键字分页查资源 (NULL appCode=全部公共)
     */
    List<ResourceDO> pageResourcesByApp(String appCode, Integer type, String keyword, int pageNum, int pageSize, long total);

    /**
     * DISTINCT 出已注册的 app_code 列表（前端下拉数据源）
     */
    List<String> listDistinctApps();

    /**
     * 持有指定资源的角色列表（详情抽屉「关联角色」tab 用）
     */
    List<RoleDO> listRolesByResource(Long resourceId);

    // ===== RBAC+Override: 用户直接授权 (白名单, 加集) =====

    /**
     * 给用户直接授权 (白名单, 加集). 幂等: 已存在则跳过.
     */
    boolean grantUserResource(Long userId, Long resourceId, String tenantCode, String grantedBy, java.time.LocalDateTime expireAt);

    /**
     * 撤销用户直接授权.
     */
    boolean revokeUserResource(Long userId, Long resourceId);

    /**
     * 查用户的直接授权列表 (白名单).
     */
    List<com.zifang.ctc.core.domain.entity.UserGrantDO> listUserGrants(Long userId);

    // ===== RBAC+Override: 用户直接拒绝 (黑名单, 减集) =====

    /**
     * 给用户设置拒绝 (黑名单, 减集). 幂等: 已存在则跳过.
     */
    boolean denyUserResource(Long userId, Long resourceId, String tenantCode, String deniedBy, java.time.LocalDateTime expireAt);

    /**
     * 撤销用户拒绝.
     */
    boolean undenyUserResource(Long userId, Long resourceId);

    /**
     * 查用户的直接拒绝列表 (黑名单).
     */
    List<com.zifang.ctc.core.domain.entity.UserDenyDO> listUserDenies(Long userId);
}
