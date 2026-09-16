package com.zifang.ctc.core.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.UserRoleDO;

import java.util.List;

/**
 * 账号域 DbService.
 * <p>
 * 纯 mapper 包装, 不做业务校验/事务/密码哈希; core.service.AccountService 负责组合业务语义.
 */
public interface AccountDbService {

    int insert(AccountDO entity);

    int updateById(AccountDO entity);

    int deleteById(Long id);

    AccountDO selectById(Long id);

    AccountDO selectByUsername(String username, String tenantCode);

    AccountDO selectByAccountNo(String accountNo);

    List<AccountDO> selectByQuery(QueryWrapper<AccountDO> wrapper);

    IPage<AccountDO> selectPage(IPage<AccountDO> page, QueryWrapper<AccountDO> wrapper);

    int updateLastLogin(Long id, String ip);

    // === user_role ===

    Long countUserRole(Long userId, Long roleId);

    int insertUserRole(UserRoleDO userRole);

    int deleteUserRoleByUserId(Long userId);

    int countUserRoleByQuery(QueryWrapper<UserRoleDO> wrapper);

    // === role 查 (用于 assignRole 校验) ===

    Long countRoleById(Long roleId);
}
