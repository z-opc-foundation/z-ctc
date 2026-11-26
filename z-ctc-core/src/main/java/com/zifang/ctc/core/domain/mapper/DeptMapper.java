package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.DeptDO;

import java.util.List;

/**
 * 部门 Mapper.
 */
public interface DeptMapper extends BaseMapper<DeptDO> {

    default DeptDO selectByDeptCode(String tenantCode, String domainCode, String deptCode) {
        return selectOne(new QueryWrapper<DeptDO>()
                .eq("tenant_code", tenantCode)
                .eq("domain_code", domainCode)
                .eq("dept_code", deptCode)
                .last("LIMIT 1"));
    }

    default List<DeptDO> selectByOrgCode(String tenantCode, String domainCode, String orgCode) {
        return selectList(new QueryWrapper<DeptDO>()
                .eq("tenant_code", tenantCode)
                .eq("domain_code", domainCode)
                .eq("org_code", orgCode)
                .orderByAsc("dept_code"));
    }
}
