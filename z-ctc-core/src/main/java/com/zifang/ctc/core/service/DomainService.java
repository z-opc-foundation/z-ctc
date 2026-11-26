package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.DomainDO;

import java.util.List;
import java.util.Optional;

/**
 * 域 Service.
 */
public interface DomainService {

    /**
     * 租户下所有域列表.
     */
    List<DomainDO> listByTenant(String tenantCode);

    /**
     * 全量列表 (管理页).
     */
    List<DomainDO> listAll();

    /**
     * 创建域.
     */
    Long createDomain(DomainDO domain, String createdBy);

    /**
     * 更新域.
     */
    boolean updateDomain(Long id, DomainDO patch, String updatedBy);

    /**
     * 删除域.
     */
    boolean deleteDomain(Long id);

    /**
     * 按 ID 查询.
     */
    Optional<DomainDO> findById(Long id);
}