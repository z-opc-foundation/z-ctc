package com.zifang.ctc.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.UserRoleDO;
import com.zifang.ctc.core.domain.mapper.AccountMapper;
import com.zifang.ctc.core.domain.mapper.RoleMapper;
import com.zifang.ctc.core.domain.mapper.UserRoleMapper;
import com.zifang.ctc.core.domain.service.AccountDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AccountDbService 默认实现：直接转发到对应 Mapper，不加任何业务语义。
 *
 * @author zifang
 * @since 1.0.0
 * @see AccountDbService
 */
@Service
public class AccountDbServiceImpl implements AccountDbService {

    private final AccountMapper accountMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    /**
     * 构造函数。
     *
     * @param accountMapper     账号数据访问对象
     * @param userRoleMapper    用户角色数据访问对象
     * @param roleMapper        角色数据访问对象
     */
    public AccountDbServiceImpl(AccountMapper accountMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper) {
        this.accountMapper = accountMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    /**
     * 插入账号记录。
     *
     * @param entity 账号实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insert(AccountDO entity) {
        return accountMapper.insert(entity);
    }

    /**
     * 根据主键更新账号记录。
     *
     * @param entity 账号实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateById(AccountDO entity) {
        return accountMapper.updateById(entity);
    }

    /**
     * 根据主键删除账号记录。
     *
     * @param id 账号主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteById(Long id) {
        return accountMapper.deleteById(id);
    }

    /**
     * 根据主键查询账号记录。
     *
     * @param id 账号主键
     * @return 账号实体对象，不存在则返回 null
     */
    @Override
    public AccountDO selectById(Long id) {
        return accountMapper.selectById(id);
    }

    /**
     * 根据用户名和租户编码查询账号记录。
     *
     * @param username  用户名
     * @param tenantCode 租户编码
     * @return 账号实体对象，不存在则返回 null
     */
    @Override
    public AccountDO selectByUsername(String username, String tenantCode) {
        return accountMapper.selectByUsername(username, tenantCode);
    }

    /**
     * 根据账号编号查询账号记录。
     *
     * @param accountNo 账号编号
     * @return 账号实体对象，不存在则返回 null
     */
    @Override
    public AccountDO selectByAccountNo(String accountNo) {
        return accountMapper.selectByAccountNo(accountNo);
    }

    /**
     * 根据查询条件查询账号列表。
     *
     * @param wrapper 查询条件包装器
     * @return 账号实体对象列表
     */
    @Override
    public List<AccountDO> selectByQuery(QueryWrapper<AccountDO> wrapper) {
        return accountMapper.selectList(wrapper);
    }

    /**
     * 分页查询账号列表。
     *
     * @param page    分页对象
     * @param wrapper 查询条件包装器
     * @return 分页的账号实体对象列表
     */
    @Override
    public IPage<AccountDO> selectPage(IPage<AccountDO> page, QueryWrapper<AccountDO> wrapper) {
        return accountMapper.selectPage(page, wrapper);
    }

    /**
     * 更新账号最后登录时间和 IP。
     *
     * @param id  账号主键
     * @param ip  登录 IP 地址
     * @return 更新成功的记录数
     */
    @Override
    public int updateLastLogin(Long id, String ip) {
        return accountMapper.updateLastLogin(id, ip);
    }

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
     * 根据用户主键删除用户角色关系记录。
     *
     * @param userId 用户主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteUserRoleByUserId(Long userId) {
        return userRoleMapper.delete(new QueryWrapper<UserRoleDO>().eq("user_id", userId));
    }

    /**
     * 根据查询条件统计用户角色关系数量。
     *
     * @param wrapper 查询条件包装器
     * @return 数量
     */
    @Override
    public int countUserRoleByQuery(QueryWrapper<UserRoleDO> wrapper) {
        return userRoleMapper.selectCount(wrapper).intValue();
    }

    /**
     * 统计角色下用户的数量。
     *
     * @param roleId 角色主键
     * @return 数量
     */
    @Override
    public Long countRoleById(Long roleId) {
        return roleMapper.selectCount(new QueryWrapper<com.zifang.ctc.core.domain.entity.RoleDO>()
                .eq("id", roleId));
    }
}
