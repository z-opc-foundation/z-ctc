package com.zifang.ctc.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.ctc.core.domain.entity.ResourceDO;
import com.zifang.ctc.core.domain.entity.RoleDO;
import com.zifang.ctc.core.domain.entity.RoleResourceDO;
import com.zifang.ctc.core.domain.entity.UserDenyDO;
import com.zifang.ctc.core.domain.entity.UserGrantDO;
import com.zifang.ctc.core.domain.entity.UserRoleDO;
import com.zifang.ctc.core.domain.mapper.ResourceMapper;
import com.zifang.ctc.core.domain.mapper.RoleMapper;
import com.zifang.ctc.core.domain.mapper.RoleResourceMapper;
import com.zifang.ctc.core.domain.mapper.UserDenyMapper;
import com.zifang.ctc.core.domain.mapper.UserGrantMapper;
import com.zifang.ctc.core.domain.mapper.UserRoleMapper;
import com.zifang.ctc.core.domain.service.AuthorizationDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AuthorizationDbService 默认实现：直接转发到对应 Mapper，不加任何业务语义。
 *
 * @author zifang
 * @since 1.0.0
 * @see AuthorizationDbService
 */
@Service
public class AuthorizationDbServiceImpl implements AuthorizationDbService {

    private final RoleMapper roleMapper;
    private final ResourceMapper resourceMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleResourceMapper roleResourceMapper;
    private final UserGrantMapper userGrantMapper;
    private final UserDenyMapper userDenyMapper;

    /**
     * 构造函数。
     *
     * @param roleMapper          角色数据访问对象
     * @param resourceMapper      资源数据访问对象
     * @param userRoleMapper      用户角色数据访问对象
     * @param roleResourceMapper  角色资源数据访问对象
     * @param userGrantMapper     用户授权数据访问对象
     * @param userDenyMapper      用户拒绝数据访问对象
     */
    public AuthorizationDbServiceImpl(RoleMapper roleMapper, ResourceMapper resourceMapper,
                                      UserRoleMapper userRoleMapper, RoleResourceMapper roleResourceMapper,
                                      UserGrantMapper userGrantMapper, UserDenyMapper userDenyMapper) {
        this.roleMapper = roleMapper;
        this.resourceMapper = resourceMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleResourceMapper = roleResourceMapper;
        this.userGrantMapper = userGrantMapper;
        this.userDenyMapper = userDenyMapper;
    }

    // ===== role =====

    /**
     * 插入角色记录。
     *
     * @param role 角色实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insertRole(RoleDO role) {
        return roleMapper.insert(role);
    }

    /**
     * 根据主键更新角色记录。
     *
     * @param role 角色实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateRoleById(RoleDO role) {
        return roleMapper.updateById(role);
    }

    /**
     * 根据主键删除角色记录。
     *
     * @param id 角色主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteRoleById(Long id) {
        return roleMapper.deleteById(id);
    }

    /**
     * 根据主键查询角色记录。
     *
     * @param id 角色主键
     * @return 角色实体对象，不存在则返回 null
     */
    @Override
    public RoleDO selectRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    /**
     * 根据角色编码和租户编码查询角色记录。
     *
     * @param roleCode   角色编码
     * @param tenantCode 租户编码
     * @return 角色实体对象，不存在则返回 null
     */
    @Override
    public RoleDO selectRoleByCode(String roleCode, String tenantCode) {
        return roleMapper.selectByRoleCode(roleCode, tenantCode);
    }

    /**
     * 根据查询条件查询角色列表。
     *
     * @param wrapper 查询条件包装器
     * @return 角色实体对象列表
     */
    @Override
    public List<RoleDO> selectRoleList(QueryWrapper<RoleDO> wrapper) {
        return roleMapper.selectList(wrapper);
    }

    /**
     * 分页查询角色列表。
     *
     * @param page    分页对象
     * @param wrapper 查询条件包装器
     * @return 分页的角色实体对象列表
     */
    @Override
    public IPage<RoleDO> selectRolePage(IPage<RoleDO> page, QueryWrapper<RoleDO> wrapper) {
        return roleMapper.selectPage(page, wrapper);
    }

    /**
     * 根据主键列表批量查询角色记录。
     *
     * @param ids 角色主键列表
     * @return 角色实体对象列表
     */
    @Override
    public List<RoleDO> selectRoleBatchIds(List<Long> ids) {
        return roleMapper.selectBatchIds(ids);
    }

    // ===== resource =====

    /**
     * 插入资源记录。
     *
     * @param resource 资源实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insertResource(ResourceDO resource) {
        return resourceMapper.insert(resource);
    }

    /**
     * 根据主键更新资源记录。
     *
     * @param resource 资源实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateResourceById(ResourceDO resource) {
        return resourceMapper.updateById(resource);
    }

    /**
     * 根据主键删除资源记录。
     *
     * @param id 资源主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteResourceById(Long id) {
        return resourceMapper.deleteById(id);
    }

    /**
     * 根据主键查询资源记录。
     *
     * @param id 资源主键
     * @return 资源实体对象，不存在则返回 null
     */
    @Override
    public ResourceDO selectResourceById(Long id) {
        return resourceMapper.selectById(id);
    }

    /**
     * 根据资源编码、应用编码和租户编码查询资源记录。
     *
     * @param resourceCode 资源编码
     * @param appCode      应用编码
     * @param tenantCode   租户编码
     * @return 资源实体对象，不存在则返回 null
     */
    @Override
    public ResourceDO selectResourceByCode(String resourceCode, String appCode, String tenantCode) {
        return resourceMapper.selectByResourceCode(resourceCode, appCode, tenantCode);
    }

    /**
     * 根据资源编码和租户编码查询资源记录（旧签名，委托三参版本）。
     *
     * @param resourceCode 资源编码
     * @param tenantCode   租户编码
     * @return 资源实体对象，不存在则返回 null
     */
    @Override
    public ResourceDO selectResourceByCode(String resourceCode, String tenantCode) {
        // 旧签名委托三参版本, appCode=null (admin 通用视图用, 业务侧请改用三参版本)
        return selectResourceByCode(resourceCode, null, tenantCode);
    }

    /**
     * 根据查询条件查询资源列表。
     *
     * @param wrapper 查询条件包装器
     * @return 资源实体对象列表
     */
    @Override
    public List<ResourceDO> selectResourceList(QueryWrapper<ResourceDO> wrapper) {
        return resourceMapper.selectList(wrapper);
    }

    /**
     * 分页查询资源列表。
     *
     * @param page    分页对象
     * @param wrapper 查询条件包装器
     * @return 分页的资源实体对象列表
     */
    @Override
    public IPage<ResourceDO> selectResourcePage(IPage<ResourceDO> page, QueryWrapper<ResourceDO> wrapper) {
        return resourceMapper.selectPage(page, wrapper);
    }

    /**
     * 根据主键列表批量查询资源记录。
     *
     * @param ids 资源主键列表
     * @return 资源实体对象列表
     */
    @Override
    public List<ResourceDO> selectResourceBatchIds(List<Long> ids) {
        return resourceMapper.selectBatchIds(ids);
    }

    /**
     * 查询所有不同的应用编码列表。
     *
     * @return 应用编码列表
     */
    @Override
    public List<String> selectDistinctApps() {
        return resourceMapper.selectDistinctApps();
    }

    // ===== user_role =====

    /**
     * 统计用户在指定角色下的数量。
     *
     * @param userId 用户主键
     * @param roleId 角色主键
     * @return 数量
     */
    @Override
    public Long countUserRole(Long userId, Long roleId) {
        return userRoleMapper.selectCount(new QueryWrapper<UserRoleDO>()
                .eq("user_id", userId).eq("role_id", roleId));
    }

    /**
     * 根据用户主键查询用户角色关系列表。
     *
     * @param userId 用户主键
     * @return 用户角色关系列表
     */
    @Override
    public List<UserRoleDO> selectUserRoleByUserId(Long userId) {
        return userRoleMapper.selectByUserId(userId);
    }

    /**
     * 插入用户角色关系记录。
     *
     * @param userRole 用户角色关系对象
     * @return 插入成功的记录数
     */
    @Override
    public int insertUserRole(UserRoleDO userRole) {
        return userRoleMapper.insert(userRole);
    }

    /**
     * 删除用户角色关系记录。
     *
     * @param userId 用户主键
     * @param roleId 角色主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteUserRole(Long userId, Long roleId) {
        return userRoleMapper.delete(new QueryWrapper<UserRoleDO>()
                .eq("user_id", userId).eq("role_id", roleId));
    }

    /**
     * 根据角色主键删除用户角色关系记录。
     *
     * @param roleId 角色主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteUserRoleByRoleId(Long roleId) {
        return userRoleMapper.delete(new QueryWrapper<UserRoleDO>().eq("role_id", roleId));
    }

    /**
     * 根据查询条件统计用户角色关系数量。
     *
     * @param wrapper 查询条件包装器
     * @return 数量
     */
    @Override
    public Long countUserRoleByQuery(QueryWrapper<UserRoleDO> wrapper) {
        return userRoleMapper.selectCount(wrapper);
    }

    // ===== role_resource =====

    /**
     * 统计角色资源关系数量。
     *
     * @param roleId     角色主键
     * @param resourceId 资源主键
     * @return 数量
     */
    @Override
    public Long countRoleResource(Long roleId, Long resourceId) {
        return roleResourceMapper.selectCount(new QueryWrapper<RoleResourceDO>()
                .eq("role_id", roleId).eq("resource_id", resourceId));
    }

    /**
     * 根据角色主键查询角色资源关系列表。
     *
     * @param roleId 角色主键
     * @return 角色资源关系列表
     */
    @Override
    public List<RoleResourceDO> selectRoleResourceByRoleId(Long roleId) {
        return roleResourceMapper.selectByRoleId(roleId);
    }

    /**
     * 根据角色 ID CSV 字符串查询资源 ID 列表。
     *
     * @param roleIdsCsv 角色 ID CSV 字符串
     * @return 资源 ID 列表
     */
    @Override
    public List<Long> selectResourceIdsByRoleIdsCsv(String roleIdsCsv) {
        return roleResourceMapper.selectResourceIdsByRoleIds(roleIdsCsv);
    }

    /**
     * 根据查询条件查询角色资源关系列表。
     *
     * @param wrapper 查询条件包装器
     * @return 角色资源关系列表
     */
    @Override
    public List<RoleResourceDO> selectRoleResourceByQuery(QueryWrapper<RoleResourceDO> wrapper) {
        return roleResourceMapper.selectList(wrapper);
    }

    /**
     * 插入角色资源关系记录。
     *
     * @param rr 角色资源关系对象
     * @return 插入成功的记录数
     */
    @Override
    public int insertRoleResource(RoleResourceDO rr) {
        return roleResourceMapper.insert(rr);
    }

    /**
     * 删除角色资源关系记录。
     *
     * @param roleId     角色主键
     * @param resourceId 资源主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteRoleResource(Long roleId, Long resourceId) {
        return roleResourceMapper.delete(new QueryWrapper<RoleResourceDO>()
                .eq("role_id", roleId).eq("resource_id", resourceId));
    }

    /**
     * 根据角色主键删除角色资源关系记录。
     *
     * @param roleId 角色主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteRoleResourceByRoleId(Long roleId) {
        return roleResourceMapper.delete(new QueryWrapper<RoleResourceDO>().eq("role_id", roleId));
    }

    /**
     * 根据资源主键删除角色资源关系记录。
     *
     * @param resourceId 资源主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteRoleResourceByResourceId(Long resourceId) {
        return roleResourceMapper.delete(new QueryWrapper<RoleResourceDO>().eq("resource_id", resourceId));
    }

    // ===== user_grant (白名单) =====

    /**
     * 插入用户授权记录（白名单）。
     *
     * @param grant 用户授权对象
     * @return 插入成功的记录数
     */
    @Override
    public int insertUserGrant(UserGrantDO grant) {
        return userGrantMapper.insert(grant);
    }

    /**
     * 删除用户授权记录。
     *
     * @param userId     用户主键
     * @param resourceId 资源主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteUserGrant(Long userId, Long resourceId) {
        return userGrantMapper.deleteByUserAndResource(userId, resourceId);
    }

    /**
     * 根据用户主键删除用户授权记录。
     *
     * @param userId 用户主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteUserGrantByUserId(Long userId) {
        return userGrantMapper.deleteByUserId(userId);
    }

    /**
     * 根据用户主键查询用户授权列表。
     *
     * @param userId 用户主键
     * @return 用户授权列表
     */
    @Override
    public List<UserGrantDO> selectUserGrantByUserId(Long userId) {
        return userGrantMapper.selectByUserId(userId);
    }

    /**
     * 根据用户主键和资源主键查询用户授权记录。
     *
     * @param userId     用户主键
     * @param resourceId 资源主键
     * @return 用户授权对象，不存在则返回 null
     */
    @Override
    public UserGrantDO selectUserGrantByUserAndResource(Long userId, Long resourceId) {
        return userGrantMapper.selectByUserAndResource(userId, resourceId);
    }

    // ===== user_deny (黑名单) =====

    /**
     * 插入用户拒绝记录（黑名单）。
     *
     * @param deny 用户拒绝对象
     * @return 插入成功的记录数
     */
    @Override
    public int insertUserDeny(UserDenyDO deny) {
        return userDenyMapper.insert(deny);
    }

    /**
     * 删除用户拒绝记录。
     *
     * @param userId     用户主键
     * @param resourceId 资源主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteUserDeny(Long userId, Long resourceId) {
        return userDenyMapper.deleteByUserAndResource(userId, resourceId);
    }

    /**
     * 根据用户主键删除用户拒绝记录。
     *
     * @param userId 用户主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteUserDenyByUserId(Long userId) {
        return userDenyMapper.deleteByUserId(userId);
    }

    /**
     * 根据用户主键查询用户拒绝列表。
     *
     * @param userId 用户主键
     * @return 用户拒绝列表
     */
    @Override
    public List<UserDenyDO> selectUserDenyByUserId(Long userId) {
        return userDenyMapper.selectByUserId(userId);
    }

    /**
     * 根据用户主键和资源主键查询用户拒绝记录。
     *
     * @param userId     用户主键
     * @param resourceId 资源主键
     * @return 用户拒绝对象，不存在则返回 null
     */
    @Override
    public UserDenyDO selectUserDenyByUserAndResource(Long userId, Long resourceId) {
        return userDenyMapper.selectByUserAndResource(userId, resourceId);
    }
}
