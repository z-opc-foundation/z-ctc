package com.zifang.ctc.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.ctc.core.domain.entity.ResourceDO;
import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.domain.entity.RoleResourceDO;
import com.zifang.ctc.core.domain.entity.UserDenyDO;
import com.zifang.ctc.core.domain.entity.UserGrantDO;
import com.zifang.ctc.core.domain.entity.UserRoleDO;
import com.zifang.ctc.core.domain.service.AuthorizationDbService;
import com.zifang.ctc.core.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AuthorizationService 默认实现：业务语义 + 事务，持久化委托给 {@link AuthorizationDbService}。
 *
 * @author zifang
 * @since 1.0.0
 * @see AuthorizationService
 */
@Service
public class CtcAuthorizationServiceImpl implements AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(CtcAuthorizationServiceImpl.class);

    private final AuthorizationDbService dbService;

    /**
     * 构造函数。
     *
     * @param dbService 授权数据服务
     */
    public CtcAuthorizationServiceImpl(AuthorizationDbService dbService) {
        this.dbService = dbService;
    }

    /**
     * 创建角色。
     *
     * @param role      角色实体对象
     * @param createdBy 创建人
     * @return 角色主键
     */
    @Override
    @Transactional
    public Long createRole(RoleDO role, String createdBy) {
        if (role.getStatus() == null) role.setStatus(1);

        role.setCreatedBy(createdBy);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        dbService.insertRole(role);
        return role.getId();
    }

    /**
     * 创建资源。
     *
     * @param resource  资源实体对象
     * @param createdBy 创建人
     * @return 资源主键
     */
    @Override
    @Transactional
    public Long createResource(ResourceDO resource, String createdBy) {
        if (resource.getStatus() == null) resource.setStatus(1);

        if (resource.getSortOrder() == null) resource.setSortOrder(0);

        resource.setCreatedBy(createdBy);
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        dbService.insertResource(resource);
        return resource.getId();
    }

    /**
     * 授权用户角色关系。
     *
     * @param userId     用户主键
     * @param roleId     角色主键
     * @param grantedBy  授权人
     * @param expireAt   过期时间
     * @return 是否授权成功（幂等：已存在则返回 true）
     */
    @Override
    @Transactional
    public boolean grantUserRole(Long userId, Long roleId, String grantedBy, LocalDateTime expireAt) {
        // 幂等: 已存在则跳过
        Long count = dbService.countUserRole(userId, roleId);
        if (count != null && count > 0) {
            return true;
        }
        UserRoleDO ur = new UserRoleDO();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setGrantedAt(LocalDateTime.now());
        ur.setGrantedBy(grantedBy);
        ur.setExpireAt(expireAt);
        dbService.insertUserRole(ur);
        log.info("CtcAuthorizationServiceImpl.grantUserRole: userId={}, roleId={}, expireAt={}", userId, roleId, expireAt);
        return true;
    }

    /**
     * 撤销用户角色关系。
     *
     * @param userId 用户主键
     * @param roleId 角色主键
     * @return 是否撤销成功
     */
    @Override
    @Transactional
    public boolean revokeUserRole(Long userId, Long roleId) {
        return dbService.deleteUserRole(userId, roleId) > 0;
    }

    /**
     * 授权角色资源关系。
     *
     * @param roleId     角色主键
     * @param resourceId 资源主键
     * @param dataFilter 数据过滤条件
     * @param grantedBy  授权人
     * @return 是否授权成功（幂等：已存在则返回 true）
     */
    @Override
    @Transactional
    public boolean grantRoleResource(Long roleId, Long resourceId, String dataFilter, String grantedBy) {
        Long count = dbService.countRoleResource(roleId, resourceId);
        if (count != null && count > 0) {
            return true;
        }
        RoleResourceDO rr = new RoleResourceDO();
        rr.setRoleId(roleId);
        rr.setResourceId(resourceId);
        rr.setDataFilter(dataFilter);
        rr.setGrantedAt(LocalDateTime.now());
        rr.setGrantedBy(grantedBy);
        dbService.insertRoleResource(rr);
        return true;
    }

    /**
     * 撤销角色资源关系。
     *
     * @param roleId     角色主键
     * @param resourceId 资源主键
     * @return 是否撤销成功
     */
    @Override
    @Transactional
    public boolean revokeRoleResource(Long roleId, Long resourceId) {
        return dbService.deleteRoleResource(roleId, resourceId) > 0;
    }

    /**
     * 查询用户的角色列表。
     *
     * @param userId 用户主键
     * @return 角色实体列表
     */
    @Override
    public List<RoleDO> listUserRoles(Long userId) {
        List<UserRoleDO> urs = dbService.selectUserRoleByUserId(userId);
        if (urs.isEmpty()) { return Collections.emptyList(); }
        List<Long> roleIds = urs.stream().map(UserRoleDO::getRoleId).collect(Collectors.toList());
        return dbService.selectRoleBatchIds(roleIds);
    }

    /**
     * 查询用户的权限列表（含角色继承、白名单、黑名单逻辑）。
     *
     * @param userId 用户主键
     * @return 资源实体列表
     */
    @Override
    public List<ResourceDO> listUserPermissions(Long userId) {
        // ── Step 1: 收集所有角色 ID (含父角色继承) ──
        Set<Long> allRoleIds = new LinkedHashSet<>();
        collectRoleIdsWithAncestors(userId, allRoleIds);
        if (allRoleIds.isEmpty()) {
            // 无角色, 但仍可能有直接 grant
            return collectDirectPermissions(userId);
        }

        // ── Step 2: 从角色权限表查资源 ID ──
        String csv = allRoleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Long> roleResourceIds = dbService.selectResourceIdsByRoleIdsCsv(csv);
        Set<Long> permIds = new LinkedHashSet<>();
        if (roleResourceIds != null) { permIds.addAll(roleResourceIds); }


        // ── Step 3: 加上用户直接授权 (白名单, 加集) ──
        List<UserGrantDO> grants = dbService.selectUserGrantByUserId(userId);
        for (UserGrantDO g : grants) permIds.add(g.getResourceId());

        // ── Step 4: 减去用户直接拒绝 (黑名单, 减集) ──
        List<UserDenyDO> denies = dbService.selectUserDenyByUserId(userId);
        Set<Long> denyIds = denies.stream().map(UserDenyDO::getResourceId).collect(Collectors.toSet());
        permIds.removeAll(denyIds);

        if (permIds.isEmpty()) { return Collections.emptyList(); }
        return dbService.selectResourceBatchIds(new ArrayList<>(permIds));
    }

    /**
     * 递归收集用户的所有角色 ID (含父角色继承)。
     *
     * @param userId 用户主键
     * @param acc    角色 ID 集合（累加器）
     */
    private void collectRoleIdsWithAncestors(Long userId, Set<Long> acc) {
        List<UserRoleDO> urs = dbService.selectUserRoleByUserId(userId);
        for (UserRoleDO ur : urs) {
            if (acc.add(ur.getRoleId())) {
                // 递归追父
                RoleDO role = dbService.selectRoleById(ur.getRoleId());
                if (role != null && role.getParentRoleId() != null && role.getParentRoleId() > 0) {
                    collectParentRoleIds(role.getParentRoleId(), acc);
                }
            }
        }
    }

    /**
     * 递归收集父角色 ID (角色继承链)。
     *
     * @param roleId 角色主键
     * @param acc    角色 ID 集合（累加器）
     */
    private void collectParentRoleIds(Long roleId, Set<Long> acc) {
        if (roleId == null || roleId <= 0 || acc.contains(roleId)) { return; }

        RoleDO role = dbService.selectRoleById(roleId);
        if (role == null) { return; }
        if (acc.add(roleId)) {
            if (role.getParentRoleId() != null && role.getParentRoleId() > 0) {
                collectParentRoleIds(role.getParentRoleId(), acc);
            }
        }
    }

    /**
     * 无角色场景：仅返回直接 grant 的权限。
     *
     * @param userId 用户主键
     * @return 资源实体列表
     */
    private List<ResourceDO> collectDirectPermissions(Long userId) {
        List<UserGrantDO> grants = dbService.selectUserGrantByUserId(userId);
        if (grants.isEmpty()) { return Collections.emptyList(); }
        List<Long> ids = grants.stream().map(UserGrantDO::getResourceId).collect(Collectors.toList());
        return dbService.selectResourceBatchIds(ids);
    }

    /**
     * 查询角色的权限列表。
     *
     * @param roleId 角色主键
     * @return 资源实体列表
     */
    @Override
    public List<ResourceDO> listRolePermissions(Long roleId) {
        List<RoleResourceDO> rrs = dbService.selectRoleResourceByRoleId(roleId);
        if (rrs.isEmpty()) { return Collections.emptyList(); }
        List<Long> resourceIds = rrs.stream().map(RoleResourceDO::getResourceId).collect(Collectors.toList());
        return dbService.selectResourceBatchIds(resourceIds);
    }

    /**
     * 检查用户是否具有指定权限。
     *
     * @param userId       用户主键
     * @param resourceCode 资源编码
     * @return 是否有权限
     */
    @Override
    public boolean checkPermission(Long userId, String resourceCode) {
        if (userId == null || resourceCode == null) { return false; }

        List<ResourceDO> permissions = listUserPermissions(userId);
        return permissions.stream().anyMatch(r -> resourceCode.equals(r.getResourceCode()));
    }

    /**
     * 根据角色编码查找角色。
     *
     * @param roleCode   角色编码
     * @param tenantCode 租户编码
     * @return 角色实体 Optional
     */
    @Override
    public Optional<RoleDO> findRoleByCode(String roleCode, String tenantCode) {
        if (roleCode == null) { return Optional.empty(); }

        return Optional.ofNullable(dbService.selectRoleByCode(roleCode, tenantCode));
    }

    /**
     * 根据资源编码、应用编码和租户编码查找资源。
     *
     * @param resourceCode 资源编码
     * @param appCode      应用编码
     * @param tenantCode   租户编码
     * @return 资源实体 Optional
     */
    @Override
    public Optional<ResourceDO> findResourceByCode(String resourceCode, String appCode, String tenantCode) {
        // FEATURE-ZTEAM-AUTH-UNIFY 段 4: DB 层强制 app_code 过滤, 业务侧声明「我要 z-team 范围的」即可消除跨 app 同名歧义.
        if (resourceCode == null) { return Optional.empty(); }

        return Optional.ofNullable(dbService.selectResourceByCode(resourceCode, appCode, tenantCode));
    }

    /**
     * 根据资源编码和租户编码查找资源（旧签名，委托三参版本）。
     *
     * @param resourceCode 资源编码
     * @param tenantCode   租户编码
     * @return 资源实体 Optional
     */
    @Override
    public Optional<ResourceDO> findResourceByCode(String resourceCode, String tenantCode) {
        // 旧签名委托三参版本, appCode=null (admin 通用视图用, 业务侧请改用三参版本)
        return findResourceByCode(resourceCode, null, tenantCode);
    }

    // ===== FEATURE014 补齐 CRUD =====

    /**
     * 查询所有角色。
     *
     * @return 角色实体列表
     */
    @Override
    public List<RoleDO> listAllRoles() {
        return dbService.selectRoleList(new QueryWrapper<RoleDO>().orderByDesc("created_at"));
    }

    /**
     * 分页查询角色列表。
     *
     * @param keyword   关键字（角色编码或名称）
     * @param pageNum   页码（从 1 开始）
     * @param pageSize  每页大小
     * @param total     总数（可选，用于预估）
     * @return 角色实体列表
     */
    @Override
    public List<RoleDO> pageRoles(String keyword, int pageNum, int pageSize, long total) {
        Page<RoleDO> page = new Page<>(pageNum, pageSize);
        QueryWrapper<RoleDO> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("role_code", keyword).or().like("role_name", keyword));
        }
        qw.orderByDesc("created_at");
        IPage<RoleDO> result = dbService.selectRolePage(page, qw);
        if (total > 0) { result.setTotal(total); }

        return result.getRecords();
    }

    /**
     * 根据主键查找角色。
     *
     * @param id 角色主键
     * @return 角色实体 Optional
     */
    @Override
    public Optional<RoleDO> findRoleById(Long id) {
        return Optional.ofNullable(dbService.selectRoleById(id));
    }

    /**
     * 更新角色。
     *
     * @param id        角色主键
     * @param patch     更新内容
     * @param updatedBy 更新人
     * @return 是否更新成功
     */
    @Override
    @Transactional
    public boolean updateRole(Long id, RoleDO patch, String updatedBy) {
        if (id == null || patch == null) { return false; }

        RoleDO exist = dbService.selectRoleById(id);
        if (exist == null) { return false; }

        if (patch.getRoleName() != null) exist.setRoleName(patch.getRoleName());

        if (patch.getParentRoleId() != null) exist.setParentRoleId(patch.getParentRoleId());

        if (patch.getTenantCode() != null) exist.setTenantCode(patch.getTenantCode());

        if (patch.getDescription() != null) exist.setDescription(patch.getDescription());

        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());

        exist.setUpdatedAt(LocalDateTime.now());
        // 4A RoleDO 没有 updated_by 字段 (列不存在), 跳过
        return dbService.updateRoleById(exist) > 0;
    }

    /**
     * 删除角色。
     *
     * @param id 角色主键
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteRole(Long id) {
        if (id == null) { return false; }

        // 解绑 user_role + role_resource
        dbService.deleteUserRoleByRoleId(id);
        dbService.deleteRoleResourceByRoleId(id);
        return dbService.deleteRoleById(id) > 0;
    }

    /**
     * 查询所有资源。
     *
     * @return 资源实体列表
     */
    @Override
    public List<ResourceDO> listAllResources() {
        return dbService.selectResourceList(new QueryWrapper<ResourceDO>().orderByAsc("sort_order").orderByAsc("id"));
    }

    /**
     * 分页查询资源列表。
     *
     * @param keyword   关键字（资源编码或名称）
     * @param pageNum   页码（从 1 开始）
     * @param pageSize  每页大小
     * @param total     总数（可选，用于预估）
     * @return 资源实体列表
     */
    @Override
    public List<ResourceDO> pageResources(String keyword, int pageNum, int pageSize, long total) {
        Page<ResourceDO> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ResourceDO> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("resource_code", keyword).or().like("resource_name", keyword));
        }
        qw.orderByAsc("sort_order").orderByAsc("id");
        IPage<ResourceDO> result = dbService.selectResourcePage(page, qw);
        if (total > 0) { result.setTotal(total); }

        return result.getRecords();
    }

    /**
     * 根据主键查找资源。
     *
     * @param id 资源主键
     * @return 资源实体 Optional
     */
    @Override
    public Optional<ResourceDO> findResourceById(Long id) {
        return Optional.ofNullable(dbService.selectResourceById(id));
    }

    /**
     * 更新资源。
     *
     * @param id        资源主键
     * @param patch     更新内容
     * @param updatedBy 更新人
     * @return 是否更新成功
     */
    @Override
    @Transactional
    public boolean updateResource(Long id, ResourceDO patch, String updatedBy) {
        if (id == null || patch == null) { return false; }

        ResourceDO exist = dbService.selectResourceById(id);
        if (exist == null) { return false; }

        if (patch.getResourceName() != null) exist.setResourceName(patch.getResourceName());

        if (patch.getResourceType() != null) exist.setResourceType(patch.getResourceType());

        if (patch.getAppCode() != null) exist.setAppCode(patch.getAppCode());
        if (patch.getParentId() != null) exist.setParentId(patch.getParentId());

        if (patch.getPath() != null) exist.setPath(patch.getPath());

        if (patch.getMethod() != null) exist.setMethod(patch.getMethod());

        if (patch.getIcon() != null) exist.setIcon(patch.getIcon());

        if (patch.getSortOrder() != null) exist.setSortOrder(patch.getSortOrder());

        if (patch.getTenantCode() != null) exist.setTenantCode(patch.getTenantCode());

        if (patch.getDescription() != null) exist.setDescription(patch.getDescription());

        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());

        exist.setUpdatedAt(LocalDateTime.now());
        return dbService.updateResourceById(exist) > 0;
    }

    /**
     * 删除资源。
     *
     * @param id 资源主键
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteResource(Long id) {
        if (id == null) { return false; }

        // 解绑 role_resource
        dbService.deleteRoleResourceByResourceId(id);
        return dbService.deleteResourceById(id) > 0;
    }

    // ===== FEATURE015: 按应用维度管理权限点 =====

    /**
     * 分页查询应用的资源列表。
     *
     * @param appCode   应用编码
     * @param type      资源类型（可选）
     * @param keyword   关键字（资源编码或名称）
     * @param pageNum   页码（从 1 开始）
     * @param pageSize  每页大小
     * @param total     总数（可选，用于预估）
     * @return 资源实体列表
     */
    @Override
    public List<ResourceDO> pageResourcesByApp(String appCode, Integer type, String keyword, int pageNum, int pageSize, long total) {
        Page<ResourceDO> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ResourceDO> qw = new QueryWrapper<>();
        if (appCode != null && !appCode.isEmpty()) {
            qw.eq("app_code", appCode);
        }
        if (type != null) {
            qw.eq("resource_type", type);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("resource_code", keyword).or().like("resource_name", keyword));
        }
        qw.orderByAsc("sort_order").orderByAsc("id");
        IPage<ResourceDO> result = dbService.selectResourcePage(page, qw);
        if (total > 0) { result.setTotal(total); }

        return result.getRecords();
    }

    /**
     * 查询所有不同的应用编码列表。
     *
     * @return 应用编码列表
     */
    @Override
    public List<String> listDistinctApps() {
        return dbService.selectDistinctApps();
    }

    /**
     * 根据资源主键查询关联的角色列表。
     *
     * @param resourceId 资源主键
     * @return 角色实体列表
     */
    @Override
    public List<RoleDO> listRolesByResource(Long resourceId) {
        if (resourceId == null) { return Collections.emptyList(); }

        List<RoleResourceDO> rrs = dbService.selectRoleResourceByQuery(
                new QueryWrapper<RoleResourceDO>().eq("resource_id", resourceId));
        if (rrs.isEmpty()) { return Collections.emptyList(); }
        List<Long> roleIds = rrs.stream().map(RoleResourceDO::getRoleId).collect(Collectors.toList());
        return dbService.selectRoleBatchIds(roleIds);
    }

    // ===== RBAC+Override: 用户直接授权 (白名单) =====

    /**
     * 授权用户资源关系（白名单）。
     *
     * @param userId     用户主键
     * @param resourceId 资源主键
     * @param tenantCode 租户编码
     * @param grantedBy  授权人
     * @param expireAt   过期时间
     * @return 是否授权成功（幂等：已存在则返回 true）
     */
    @Override
    @Transactional
    public boolean grantUserResource(Long userId, Long resourceId, String tenantCode, String grantedBy, LocalDateTime expireAt) {
        if (userId == null || resourceId == null) { return false; }

        // 幂等: 已存在则跳过
        UserGrantDO exist = dbService.selectUserGrantByUserAndResource(userId, resourceId);
        if (exist != null) { return true; }

        UserGrantDO grant = new UserGrantDO();
        grant.setUserId(userId);
        grant.setResourceId(resourceId);
        grant.setTenantCode(tenantCode);
        grant.setGrantedAt(LocalDateTime.now());
        grant.setGrantedBy(grantedBy);
        grant.setExpireAt(expireAt);
        dbService.insertUserGrant(grant);
        log.info("grantUserResource: userId={}, resourceId={}, expireAt={}", userId, resourceId, expireAt);
        return true;
    }

    /**
     * 撤销用户资源关系（白名单）。
     *
     * @param userId     用户主键
     * @param resourceId 资源主键
     * @return 是否撤销成功
     */
    @Override
    @Transactional
    public boolean revokeUserResource(Long userId, Long resourceId) {
        return dbService.deleteUserGrant(userId, resourceId) > 0;
    }

    /**
     * 查询用户的授权列表（白名单）。
     *
     * @param userId 用户主键
     * @return 用户授权列表
     */
    @Override
    public List<UserGrantDO> listUserGrants(Long userId) {
        return dbService.selectUserGrantByUserId(userId);
    }

    // ===== RBAC+Override: 用户直接拒绝 (黑名单) =====

    /**
     * 拒绝用户资源关系（黑名单）。
     *
     * @param userId     用户主键
     * @param resourceId 资源主键
     * @param tenantCode 租户编码
     * @param deniedBy   拒绝人
     * @param expireAt   过期时间
     * @return 是否拒绝成功（幂等：已存在则返回 true）
     */
    @Override
    @Transactional
    public boolean denyUserResource(Long userId, Long resourceId, String tenantCode, String deniedBy, LocalDateTime expireAt) {
        if (userId == null || resourceId == null) { return false; }

        // 幂等
        UserDenyDO exist = dbService.selectUserDenyByUserAndResource(userId, resourceId);
        if (exist != null) { return true; }

        UserDenyDO deny = new UserDenyDO();
        deny.setUserId(userId);
        deny.setResourceId(resourceId);
        deny.setTenantCode(tenantCode);
        deny.setDeniedAt(LocalDateTime.now());
        deny.setDeniedBy(deniedBy);
        deny.setExpireAt(expireAt);
        dbService.insertUserDeny(deny);
        log.info("denyUserResource: userId={}, resourceId={}, expireAt={}", userId, resourceId, expireAt);
        return true;
    }

    /**
     * 撤销用户资源拒绝关系（黑名单）。
     *
     * @param userId     用户主键
     * @param resourceId 资源主键
     * @return 是否撤销成功
     */
    @Override
    @Transactional
    public boolean undenyUserResource(Long userId, Long resourceId) {
        return dbService.deleteUserDeny(userId, resourceId) > 0;
    }

    /**
     * 查询用户的拒绝列表（黑名单）。
     *
     * @param userId 用户主键
     * @return 用户拒绝列表
     */
    @Override
    public List<UserDenyDO> listUserDenies(Long userId) {
        return dbService.selectUserDenyByUserId(userId);
    }
}
