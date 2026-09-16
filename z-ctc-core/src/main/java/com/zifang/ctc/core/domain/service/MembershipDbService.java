package com.zifang.ctc.core.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.MembershipDO;

import java.util.List;

/**
 * z_ctc_ac_membership 域 DbService (FEATURE049).
 * <p>
 * User × Tenant 多对多关系的纯 mapper 包装.
 */
public interface MembershipDbService {

    int insert(MembershipDO entity);

    int updateById(MembershipDO entity);

    int deleteById(Long id);

    MembershipDO selectById(Long id);

    /**
     * 按 user_id + tenant_code 查
     */
    MembershipDO selectByUserAndTenant(Long userId, String tenantCode);

    /**
     * 查 user 的所有 active 成员关系
     */
    List<MembershipDO> selectActiveByUserId(Long userId);

    /**
     * 查 user 的所有成员关系 (含历史)
     */
    List<MembershipDO> selectAllByUserId(Long userId);

    /**
     * 查某租户的所有 active 成员
     */
    List<MembershipDO> selectActiveByTenant(String tenantCode);

    /**
     * 查 user 的 active 租户数量
     */
    long countActiveByUserId(Long userId);

    List<MembershipDO> selectByQuery(QueryWrapper<MembershipDO> wrapper);
}
