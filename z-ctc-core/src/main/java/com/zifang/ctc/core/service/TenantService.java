package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.TenantDO;

import java.util.List;
import java.util.Optional;

/**
 * 租户服务接口.
 */
public interface TenantService {

    Long createTenant(TenantDO tenant, String createdBy);

    Optional<TenantDO> findByTenantCode(String tenantCode);

    boolean updateStatus(String tenantCode, int newStatus);

    // FEATURE014: 补齐 CRUD

    /**
     * 全量列表
     */
    List<TenantDO> listAll();

    /**
     * 分页查询
     */
    List<TenantDO> pageList(String keyword, int pageNum, int pageSize, long total);

    /**
     * 更新 (不含 status)
     */
    boolean updateTenant(String tenantCode, TenantDO patch);

    /**
     * 删除
     */
    boolean deleteTenant(String tenantCode);
}
