package com.zifang.ctc.core.domain.service;

import com.zifang.ctc.core.domain.entity.OrgDO;

import java.util.List;

/**
 * 组织域 DbService.
 */
public interface OrgDbService {

    int insert(OrgDO entity);

    int updateById(OrgDO entity);

    int deleteById(Long id);

    OrgDO selectByOrgCode(String tenantCode, String domainCode, String orgCode);

    List<OrgDO> selectByDomainCode(String tenantCode, String domainCode);
}
