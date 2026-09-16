package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.DeptDO;

import java.util.List;
import java.util.Optional;

/**
 * 部门服务接口.
 */
public interface DeptService {

    /**
     * 创建部门
     */
    Long createDept(DeptDO dept, String createdBy);

    /**
     * 按 (tenant, domain, deptCode) 查询
     */
    Optional<DeptDO> findByCode(String tenantCode, String domainCode, String deptCode);

    /**
     * 按组织列出全部部门
     */
    List<DeptDO> listByOrg(String tenantCode, String domainCode, String orgCode);

    /**
     * 更新名称/描述/状态/扩展配置
     */
    boolean updateDept(String tenantCode, String domainCode, String deptCode, DeptDO patch, String updatedBy);

    /**
     * 删除部门
     */
    boolean deleteDept(String tenantCode, String domainCode, String deptCode);
}