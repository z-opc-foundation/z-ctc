package com.zifang.ctc.core.domain.service;

import com.zifang.ctc.core.domain.entity.DeptDO;

import java.util.List;

/**
 * 部门域 DbService.
 */
public interface DeptDbService {

    int insert(DeptDO entity);

    int updateById(DeptDO entity);

    int deleteById(Long id);

    DeptDO selectByDeptCode(String tenantCode, String domainCode, String deptCode);

    List<DeptDO> selectByOrgCode(String tenantCode, String domainCode, String orgCode);
}
