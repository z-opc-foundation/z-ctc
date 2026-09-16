package com.zifang.ctc.core.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.ctc.core.domain.entity.ResourceDO;
import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.domain.entity.RoleResourceDO;
import com.zifang.ctc.core.domain.entity.UserDenyDO;
import com.zifang.ctc.core.domain.entity.UserGrantDO;
import com.zifang.ctc.core.domain.entity.UserRoleDO;

import java.util.List;

/**
 * 授权域 DbService — RBAC 四表 + Override 二表 (grant / deny) 全部在此.
 * <p>
 * 业务语义 (角色 CRUD / 权限检查 / 树形查询) 留在 core.service.AuthorizationService.
 */
public interface AuthorizationDbService {

    // === role ===

    int insertRole(RoleDO role);

    int updateRoleById(RoleDO role);

    int deleteRoleById(Long id);

    RoleDO selectRoleById(Long id);

    RoleDO selectRoleByCode(String roleCode, String tenantCode);

    List<RoleDO> selectRoleList(QueryWrapper<RoleDO> wrapper);

    IPage<RoleDO> selectRolePage(IPage<RoleDO> page, QueryWrapper<RoleDO> wrapper);

    List<RoleDO> selectRoleBatchIds(List<Long> ids);

    // === resource ===

    int insertResource(ResourceDO resource);

    int updateResourceById(ResourceDO resource);

    int deleteResourceById(Long id);

    ResourceDO selectResourceById(Long id);

    /**
     * 按 resource_code + 可选 app_code + 可选 tenant_code 查资源.
     * <p>
     * 业务侧已知自己所属 app 时必须传 appCode, 避免跨 app 同名资源命中歧义 (FEATURE-ZTEAM-AUTH-UNIFY 段 4).
     *
     * @param resourceCode 资源编码
     * @param appCode      应用编码; null/空表示不过滤
     * @param tenantCode   租户编码; null 表示不过滤
     */
    ResourceDO selectResourceByCode(String resourceCode, String appCode, String tenantCode);

    /**
     * 旧签名委托新签名 (appCode=null), 仅用于 admin 通用视图. 业务侧请改用三参版本.
     */
    ResourceDO selectResourceByCode(String resourceCode, String tenantCode);

    List<ResourceDO> selectResourceList(QueryWrapper<ResourceDO> wrapper);

    IPage<ResourceDO> selectResourcePage(IPage<ResourceDO> page, QueryWrapper<ResourceDO> wrapper);

    List<ResourceDO> selectResourceBatchIds(List<Long> ids);

    List<String> selectDistinctApps();

    // === user_role ===

    Long countUserRole(Long userId, Long roleId);

    List<UserRoleDO> selectUserRoleByUserId(Long userId);

    int insertUserRole(UserRoleDO userRole);

    int deleteUserRole(Long userId, Long roleId);

    int deleteUserRoleByRoleId(Long roleId);

    Long countUserRoleByQuery(QueryWrapper<UserRoleDO> wrapper);

    // === role_resource ===

    Long countRoleResource(Long roleId, Long resourceId);

    List<RoleResourceDO> selectRoleResourceByRoleId(Long roleId);

    List<Long> selectResourceIdsByRoleIdsCsv(String roleIdsCsv);

    List<RoleResourceDO> selectRoleResourceByQuery(QueryWrapper<RoleResourceDO> wrapper);

    int insertRoleResource(RoleResourceDO rr);

    int deleteRoleResource(Long roleId, Long resourceId);

    int deleteRoleResourceByRoleId(Long roleId);

    int deleteRoleResourceByResourceId(Long resourceId);

    // === user_grant (白名单, 加集) ===

    int insertUserGrant(UserGrantDO grant);

    int deleteUserGrant(Long userId, Long resourceId);

    int deleteUserGrantByUserId(Long userId);

    List<UserGrantDO> selectUserGrantByUserId(Long userId);

    UserGrantDO selectUserGrantByUserAndResource(Long userId, Long resourceId);

    // === user_deny (黑名单, 减集) ===

    int insertUserDeny(UserDenyDO deny);

    int deleteUserDeny(Long userId, Long resourceId);

    int deleteUserDenyByUserId(Long userId);

    List<UserDenyDO> selectUserDenyByUserId(Long userId);

    UserDenyDO selectUserDenyByUserAndResource(Long userId, Long resourceId);
}
