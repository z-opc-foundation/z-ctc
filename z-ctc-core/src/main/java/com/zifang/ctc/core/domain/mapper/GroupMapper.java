package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.GroupDO;

import java.util.List;

/**
 * 组别 Mapper.
 */
public interface GroupMapper extends BaseMapper<GroupDO> {

    default GroupDO selectByGroupCode(String tenantCode, String domainCode, String groupCode) {
        return selectOne(new QueryWrapper<GroupDO>()
                .eq("tenant_code", tenantCode)
                .eq("domain_code", domainCode)
                .eq("group_code", groupCode)
                .last("LIMIT 1"));
    }

    default List<GroupDO> selectByDeptCode(String tenantCode, String domainCode, String deptCode) {
        return selectList(new QueryWrapper<GroupDO>()
                .eq("tenant_code", tenantCode)
                .eq("domain_code", domainCode)
                .eq("dept_code", deptCode)
                .orderByAsc("group_code"));
    }
}
