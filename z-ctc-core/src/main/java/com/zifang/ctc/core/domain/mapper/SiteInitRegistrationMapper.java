package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.SiteInitRegistrationDO;

/**
 * 建站初始化域注册 Mapper.
 */
public interface SiteInitRegistrationMapper extends BaseMapper<SiteInitRegistrationDO> {

    default SiteInitRegistrationDO selectByDomainCode(String domainCode) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SiteInitRegistrationDO>()
                .eq("domain_code", domainCode)
                .last("LIMIT 1"));
    }
}
