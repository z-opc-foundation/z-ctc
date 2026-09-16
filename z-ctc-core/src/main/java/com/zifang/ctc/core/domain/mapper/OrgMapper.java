package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.OrgDO;

import java.util.List;

/**
 * 组织 Mapper.
 */
public interface OrgMapper extends BaseMapper<OrgDO> {

    default OrgDO selectByOrgCode(String tenantCode, String domainCode, String orgCode) {
        return selectOne(new QueryWrapper<OrgDO>()
                .eq("tenant_code", tenantCode)
                .eq("domain_code", domainCode)
                .eq("org_code", orgCode)
                .last("LIMIT 1"));
    }

    default List<OrgDO> selectByDomainCode(String tenantCode, String domainCode) {
        return selectList(new QueryWrapper<OrgDO>()
                .eq("tenant_code", tenantCode)
                .eq("domain_code", domainCode)
                .orderByAsc("org_code"));
    }
}
