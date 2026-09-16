package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.OrgDO;

import java.util.List;
import java.util.Optional;

/**
 * 组织服务接口.
 */
public interface OrgService {

    /**
     * 创建组织. 同一 (tenant, domain, orgCode) 已存在则抛 IllegalStateException
     */
    Long createOrg(OrgDO org, String createdBy);

    /**
     * 按 (tenant, domain, orgCode) 查询
     */
    Optional<OrgDO> findByCode(String tenantCode, String domainCode, String orgCode);

    /**
     * 按域列出全部组织
     */
    List<OrgDO> listByDomain(String tenantCode, String domainCode);

    /**
     * 更新名称/描述/状态/扩展配置 (orgCode 不允许修改)
     */
    boolean updateOrg(String tenantCode, String domainCode, String orgCode, OrgDO patch, String updatedBy);

    /**
     * 删除组织 (级联删除部门 + 组别由 controller 处理)
     */
    boolean deleteOrg(String tenantCode, String domainCode, String orgCode);
}