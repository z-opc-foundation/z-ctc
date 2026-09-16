package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.AccountDO;

import java.util.List;
import java.util.Optional;

/**
 * 账号服务接口.
 */
public interface AccountService {

    Long createAccount(AccountDO account, String rawPassword);

    Optional<AccountDO> findById(Long id);

    Optional<AccountDO> findByUsername(String username, String tenantCode);

    Optional<AccountDO> findByAccountNo(String accountNo);

    List<AccountDO> listByTenant(String tenantCode, int pageNum, int pageSize);

    boolean updateStatus(Long id, int newStatus, String updatedBy);

    boolean changePassword(Long id, String oldRawPassword, String newRawPassword);

    boolean resetPassword(Long id, String newRawPassword, String updatedBy);

    // FEATURE014: 补齐 CRUD

    /**
     * 全量列表 (无分页, 兼容老前端)
     */
    List<AccountDO> listAll();

    /**
     * 分页查询
     */
    List<AccountDO> pageList(String tenantCode, String keyword, int pageNum, int pageSize, long total);

    /**
     * 按 ID 更新 (不含密码)
     */
    boolean updateAccount(Long id, AccountDO patch, String updatedBy);

    /**
     * 软删除 (status=0) — 这里直接物理删除, 4A 后续可加逻辑删除
     */
    boolean deleteAccount(Long id);

    /**
     * 分配角色 (复用 z-ctc-authz 的 user_role 表)
     */
    boolean assignRole(Long userId, Long roleId, String grantedBy);
}
