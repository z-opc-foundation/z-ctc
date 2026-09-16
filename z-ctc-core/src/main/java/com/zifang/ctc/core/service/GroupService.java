package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.GroupDO;

import java.util.List;
import java.util.Optional;

/**
 * 组别服务接口.
 */
public interface GroupService {

    /**
     * 创建组别
     */
    Long createGroup(GroupDO group, String createdBy);

    /**
     * 按 (tenant, domain, groupCode) 查询
     */
    Optional<GroupDO> findByCode(String tenantCode, String domainCode, String groupCode);

    /**
     * 按部门列出全部组别
     */
    List<GroupDO> listByDept(String tenantCode, String domainCode, String deptCode);

    /**
     * 更新名称/描述/状态/扩展配置
     */
    boolean updateGroup(String tenantCode, String domainCode, String groupCode, GroupDO patch, String updatedBy);

    /**
     * 删除组别
     */
    boolean deleteGroup(String tenantCode, String domainCode, String groupCode);
}