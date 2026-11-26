package com.zifang.ctc.core.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.UserDO;

import java.util.List;

/**
 * z_ctc_ac_user 域 DbService (FEATURE049).
 * <p>
 * 纯 mapper 包装, 不做业务校验/事务. 业务语义由 core.service.UserService 负责.
 */
public interface UserDbService {

    int insert(UserDO entity);

    int updateById(UserDO entity);

    int deleteById(Long id);

    UserDO selectById(Long id);

    UserDO selectByUserNo(String userNo);

    UserDO selectByEmail(String email);

    UserDO selectByPhone(String phone);

    UserDO selectByUsername(String username);

    /**
     * 通用条件查询, 给 Service 层做组合条件用
     */
    List<UserDO> selectByQuery(QueryWrapper<UserDO> wrapper);
}
