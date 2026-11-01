package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.MembershipDO;

import java.util.List;
import java.util.Optional;

/**
 * 租户成员关系服务 (FEATURE049).
 * <p>
 * User × Tenant 多对多关系管理. 是"身份与租户解耦"模型的核心枢纽.
 */
public interface MembershipService {

    /**
     * 建立成员关系 (默认 member_status=1, joined_at=now).
     * 重复建立返回原记录 (幂等).
     */
    MembershipDO create(Long userId, String tenantCode, String roleCode, String deptCode);

    /**
     * owner 专属: 创建租户时同时建立 owner membership.
     */
    MembershipDO createOwner(Long userId, String tenantCode);

    Optional<MembershipDO> findByUserAndTenant(Long userId, String tenantCode);

    /**
     * 用户是否为该租户的活跃成员 (member_status = 1).
     * <p>
     * 用于"能不能看这个租户的数据"的判定, 不区分租户内角色.
     */
    boolean isMember(Long userId, String tenantCode);

    /**
     * 用户是否能在该租户下做管理操作 (活跃成员 且 role_code ∈ {owner, admin}).
     * <p>
     * 用于"能不能改这个租户的数据"的判定. 平台管理员不走这里, 由调用方另判.
     */
    boolean isTenantAdmin(Long userId, String tenantCode);

    Optional<MembershipDO> findById(Long id);

    List<MembershipDO> listActiveByUserId(Long userId);

    List<MembershipDO> listAllByUserId(Long userId);

    List<MembershipDO> listActiveByTenant(String tenantCode);

    long countActiveByUserId(Long userId);

    /**
     * 软删除成员关系 (owner/admin 操作).
     * member_status 置为 2 (已禁用) 或 3 (已退出), 写 left_at.
     */
    boolean disable(Long id, int newStatus);

    /**
     * 移除成员 (admin 业务) — 默认 member_status=2 (已禁用).
     */
    boolean remove(Long id);

    /**
     * 用户主动退出 — 默认 member_status=3 (已退出).
     * owner 不能退出, 应在调用方校验.
     */
    boolean leave(Long id);
}
