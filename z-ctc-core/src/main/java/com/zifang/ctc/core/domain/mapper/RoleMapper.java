package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.RoleDO;

public interface RoleMapper extends BaseMapper<RoleDO> {
    default RoleDO selectByRoleCode(String roleCode, String tenantCode) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RoleDO>()
                .eq("role_code", roleCode)
                .eq(tenantCode != null, "tenant_code", tenantCode)
                .last("LIMIT 1"));
    }
}
