package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.TenantDO;

/**
 * 租户 Mapper.
 */
public interface TenantMapper extends BaseMapper<TenantDO> {

    default TenantDO selectByTenantCode(String tenantCode) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TenantDO>()
                .eq("tenant_code", tenantCode)
                .last("LIMIT 1"));
    }
}
