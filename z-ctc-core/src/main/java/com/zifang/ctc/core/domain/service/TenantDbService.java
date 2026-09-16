package com.zifang.ctc.core.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.ctc.core.domain.entity.TenantDO;

import java.util.List;

/**
 * 租户域 DbService.
 */
public interface TenantDbService {

    int insert(TenantDO entity);

    int updateById(TenantDO entity);

    int deleteById(Long id);

    TenantDO selectByTenantCode(String tenantCode);

    List<TenantDO> selectByQuery(QueryWrapper<TenantDO> wrapper);

    IPage<TenantDO> selectPage(IPage<TenantDO> page, QueryWrapper<TenantDO> wrapper);
}
