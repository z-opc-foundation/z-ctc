package com.zifang.ctc.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.UserRoleDO;
import com.zifang.ctc.core.domain.service.AccountDbService;
import com.zifang.ctc.core.service.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 账号服务实现类.
 * <p>
 * BCrypt 密码哈希, 业务语义（账号状态/过期/租户校验） + 事务,
 * 持久化委托给 {@link AccountDbService}.
 *
 * @author zifang
 * @since 1.0.0
 * @see AccountService
 * @see AccountDbService
 */
@Service
public class AcAccountServiceImpl implements AccountService {

    private static final Logger log = LogManager.getLogger(AcAccountServiceImpl.class);

    private final AccountDbService accountDbService;

    public AcAccountServiceImpl(AccountDbService accountDbService) {
        this.accountDbService = accountDbService;
    }

    /**
     * 创建账号（含密码哈希）.
     *
     * @param account   账号信息
     * @param rawPassword 原始密码（会自动 BCrypt 哈希）
     * @return 账号ID
     * @throws IllegalArgumentException 当 account 或 rawPassword 为空时
     */
    @Override
    @Transactional
    public Long createAccount(AccountDO account, String rawPassword) {
        if (account == null) {
            throw new IllegalArgumentException("account must not be null");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("rawPassword must not be empty");
        }
        if (account.getStatus() == null) {
            account.setStatus(1);
        }
        if (account.getAccountType() == null) {
            account.setAccountType(3);
        }
        account.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountDbService.insert(account);
        log.info("AcAccountServiceImpl.createAccount: id={}, username={}, tenant={}",
                account.getId(), account.getUsername(), account.getTenantCode());
        return account.getId();
    }

    /**
     * 根据ID查询账号.
     *
     * @param id 账号ID
     * @return 账号信息，不存在则返回空
     */
    @Override
    public Optional<AccountDO> findById(Long id) {
        if (id == null) { return Optional.empty(); }

        return Optional.ofNullable(accountDbService.selectById(id));
    }

    /**
     * 根据用户名和租户编码查询账号.
     *
     * @param username   用户名
     * @param tenantCode 租户编码
     * @return 账号信息，不存在则返回空
     */
    @Override
    public Optional<AccountDO> findByUsername(String username, String tenantCode) {
        if (username == null) { return Optional.empty(); }

        return Optional.ofNullable(accountDbService.selectByUsername(username, tenantCode));
    }

    /**
     * 根据账号编号查询账号.
     *
     * @param accountNo 账号编号
     * @return 账号信息，不存在则返回空
     */
    @Override
    public Optional<AccountDO> findByAccountNo(String accountNo) {
        if (accountNo == null) { return Optional.empty(); }

        return Optional.ofNullable(accountDbService.selectByAccountNo(accountNo));
    }

    /**
     * 分页查询租户下的账号.
     *
     * @param tenantCode 租户编码
     * @param pageNum    页码（从 1 开始）
     * @param pageSize   每页大小
     * @return 账号列表
     */
    @Override
    public List<AccountDO> listByTenant(String tenantCode, int pageNum, int pageSize) {
        Page<AccountDO> page = new Page<>(pageNum, pageSize);
        QueryWrapper<AccountDO> qw = new QueryWrapper<>();
        if (tenantCode != null) {
            qw.eq("tenant_code", tenantCode);
        }
        qw.orderByDesc("created_at");
        return accountDbService.selectPage(page, qw).getRecords();
    }

    /**
     * 更新账号状态.
     *
     * @param id        账号ID
     * @param newStatus 新状态
     * @param updatedBy 更新人
     * @return true 更新成功，false 账号不存在或更新失败
     */
    @Override
    @Transactional
    public boolean updateStatus(Long id, int newStatus, String updatedBy) {
        AccountDO account = accountDbService.selectById(id);
        if (account == null) { return false; }

        account.setStatus(newStatus);
        account.setUpdatedAt(LocalDateTime.now());
        account.setUpdatedBy(updatedBy);
        return accountDbService.updateById(account) > 0;
    }

    /**
     * 修改密码（需验证旧密码）.
     *
     * @param id              账号ID
     * @param oldRawPassword  旧密码
     * @param newRawPassword  新密码
     * @return true 修改成功，false 账号不存在、旧密码错误或修改失败
     */
    @Override
    @Transactional
    public boolean changePassword(Long id, String oldRawPassword, String newRawPassword) {
        AccountDO account = accountDbService.selectById(id);
        if (account == null) { return false; }

        if (account.getPasswordHash() == null) return false;

        if (!BCrypt.checkpw(oldRawPassword, account.getPasswordHash())) {
            log.warn("AcAccountServiceImpl.changePassword 旧密码错误: id={}", id);
            return false;
        }
        account.setPasswordHash(BCrypt.hashpw(newRawPassword, BCrypt.gensalt()));
        account.setUpdatedAt(LocalDateTime.now());
        return accountDbService.updateById(account) > 0;
    }

    /**
     * 重置密码（无需验证旧密码）.
     *
     * @param id              账号ID
     * @param newRawPassword  新密码
     * @param updatedBy       更新人
     * @return true 重置成功，false 账号不存在或重置失败
     */
    @Override
    @Transactional
    public boolean resetPassword(Long id, String newRawPassword, String updatedBy) {
        AccountDO account = accountDbService.selectById(id);
        if (account == null) { return false; }

        account.setPasswordHash(BCrypt.hashpw(newRawPassword, BCrypt.gensalt()));
        account.setUpdatedAt(LocalDateTime.now());
        account.setUpdatedBy(updatedBy);
        return accountDbService.updateById(account) > 0;
    }

    // ===== FEATURE014 补齐 CRUD =====

    /**
     * 查询所有账号（按创建时间倒序）.
     *
     * @return 账号列表
     */
    @Override
    public List<AccountDO> listAll() {
        return accountDbService.selectByQuery(new QueryWrapper<AccountDO>().orderByDesc("created_at"));
    }

    /**
     * 分页查询账号（支持关键词搜索）.
     *
     * @param tenantCode 租户编码
     * @param keyword    关键词（username/nickname/email 模糊匹配）
     * @param pageNum    页码（从 1 开始）
     * @param pageSize   每页大小
     * @param total      总记录数（用于分页组件，非必须）
     * @return 账号列表
     */
    @Override
    public List<AccountDO> pageList(String tenantCode, String keyword, int pageNum, int pageSize, long total) {
        Page<AccountDO> page = new Page<>(pageNum, pageSize);
        QueryWrapper<AccountDO> qw = new QueryWrapper<>();
        if (tenantCode != null && !tenantCode.isEmpty()) { qw.eq("tenant_code", tenantCode); }

        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("username", keyword).or().like("nickname", keyword).or().like("email", keyword));
        }
        qw.orderByDesc("created_at");
        IPage<AccountDO> result = accountDbService.selectPage(page, qw);
        if (total > 0) { result.setTotal(total); }

        return result.getRecords();
    }

    /**
     * 更新账号信息.
     *
     * @param id        账号ID
     * @param patch     更新内容
     * @param updatedBy 更新人
     * @return true 更新成功，false 账号不存在或更新失败
     */
    @Override
    @Transactional
    public boolean updateAccount(Long id, AccountDO patch, String updatedBy) {
        if (id == null || patch == null) { return false; }

        AccountDO exist = accountDbService.selectById(id);
        if (exist == null) { return false; }

        if (patch.getNickname() != null) exist.setNickname(patch.getNickname());

        if (patch.getEmail() != null) exist.setEmail(patch.getEmail());

        if (patch.getPhone() != null) exist.setPhone(patch.getPhone());

        if (patch.getAccountType() != null) exist.setAccountType(patch.getAccountType());

        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());

        if (patch.getTenantCode() != null) exist.setTenantCode(patch.getTenantCode());

        if (patch.getExpireAt() != null) exist.setExpireAt(patch.getExpireAt());

        exist.setUpdatedAt(LocalDateTime.now());
        exist.setUpdatedBy(updatedBy);
        return accountDbService.updateById(exist) > 0;
    }

    /**
     * 删除账号.
     *
     * @param id 账号ID
     * @return true 删除成功，false 账号不存在或删除失败
     */
    @Override
    @Transactional
    public boolean deleteAccount(Long id) {
        if (id == null) { return false; }

        // 解绑 user_role
        accountDbService.deleteUserRoleByUserId(id);
        // 物理删除 account (z-ctc-ac 没设逻辑删除, 直接删)
        return accountDbService.deleteById(id) > 0;
    }

    /**
     * 分配角色给账号.
     *
     * @param userId    用户/账号ID
     * @param roleId    角色ID
     * @param grantedBy 授权人
     * @return true 分配成功（已存在则返回 true），false 分配失败
     */
    @Override
    @Transactional
    public boolean assignRole(Long userId, Long roleId, String grantedBy) {
        if (userId == null || roleId == null) { return false; }

        // 校验 account / role 存在
        if (accountDbService.selectById(userId) == null) return false;

        if (accountDbService.countRoleById(roleId) == null
                || accountDbService.countRoleById(roleId) == 0) return false;
        // 已存在则跳过
        Long cnt = accountDbService.countUserRole(userId, roleId);
        if (cnt != null && cnt > 0) { return true; }

        UserRoleDO ur = new UserRoleDO();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setGrantedBy(grantedBy);
        ur.setGrantedAt(LocalDateTime.now());
        accountDbService.insertUserRole(ur);
        return true;
    }
}
