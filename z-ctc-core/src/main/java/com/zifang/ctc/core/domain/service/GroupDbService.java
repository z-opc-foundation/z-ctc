package com.zifang.ctc.core.domain.service;

import com.zifang.ctc.core.domain.entity.GroupDO;

import java.util.List;

/**
 * 组别域 DbService.
 */
public interface GroupDbService {

    int insert(GroupDO entity);

    int updateById(GroupDO entity);

    int deleteById(Long id);

    GroupDO selectByGroupCode(String tenantCode, String domainCode, String groupCode);

    List<GroupDO> selectByDeptCode(String tenantCode, String domainCode, String deptCode);
}
