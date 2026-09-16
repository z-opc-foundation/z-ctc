package com.zifang.ctc.core.domain.service;

import com.zifang.ctc.core.domain.entity.DomainDO;

import java.util.List;

/**
 * 域 DbService.
 */
public interface DomainDbService {

    List<DomainDO> selectByTenant(String tenantCode);

    List<DomainDO> selectAll();

    DomainDO selectById(Long id);

    DomainDO selectByDomainCode(String tenantCode, String domainCode);

    int insert(DomainDO entity);

    int updateById(DomainDO entity);

    int deleteById(Long id);
}
