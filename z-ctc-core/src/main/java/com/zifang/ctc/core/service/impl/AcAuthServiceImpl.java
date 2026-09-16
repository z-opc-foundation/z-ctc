package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.AuthDO;
import com.zifang.ctc.core.domain.service.AuthDbService;
import com.zifang.ctc.core.service.AccountWithAuth;
import com.zifang.ctc.core.service.AuthService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 凭证 + 登录服务实现类.
 * <p>
 * 业务语义 (账号状态/过期/租户校验 + BCrypt 哈希) 留在本层,
 * 持久化委托给 {@link AuthDbService}.
 *
 * @author zifang
 * @since 1.0.0
 * @see AuthService
 * @see AuthDbService
 */
@Service
public class AcAuthServiceImpl implements AuthService {

    private static final Logger log = LogManager.getLogger(AcAuthServiceImpl.class);

    private final AuthDbService authDbService;

    public AcAuthServiceImpl(AuthDbService authDbService) {
        this.authDbService = authDbService;
    }

    /**
     * 根据身份类型和标识符查找账号（含凭证信息）.
     *
     * @param identityType 身份类型（如：1=密码）
     * @param identifier   标识符（如：邮箱/手机号）
     * @param tenantCode   租户编码（可选）
     * @return 账号信息（含凭证），不存在或状态异常返回空
     */
    @Override
    public Optional<AccountWithAuth> findAccountByCredential(int identityType, String identifier, String tenantCode) {
        if (identifier == null) { return Optional.empty(); }

        AuthDO auth = authDbService.selectByIdentity(identityType, identifier);
        if (auth == null || auth.getStatus() == null || auth.getStatus() != 1) {
            return Optional.empty();
        }
        AccountDO account = authDbService.selectAccountById(auth.getUserId()); // FEATURE049: 兼容旧 account 表, user_id == account.id
        if (account == null) { return Optional.empty(); }

        if (account.getStatus() == null || account.getStatus() != 1) {
            return Optional.empty();
        }
        if (account.getExpireAt() != null && account.getExpireAt().isBefore(LocalDateTime.now())) {
            log.debug("AcAuthServiceImpl.findAccountByCredential 账号已过期: accountId={}", account.getId());
            return Optional.empty();
        }
        if (tenantCode != null && !tenantCode.isEmpty()
                && account.getTenantCode() != null
                && !account.getTenantCode().equals(tenantCode)) {
            return Optional.empty();
        }
        return Optional.of(new AccountWithAuth(account, auth));
    }

    /**
     * 验证密码是否匹配.
     *
     * @param rawPassword  原始密码
     * @param storedHash   存储的哈希值
     * @return true 密码匹配，false 不匹配或异常
     */
    @Override
    public boolean verifyPassword(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) { return false; }

        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (Exception e) {
            log.warn("AcAuthServiceImpl.verifyPassword 异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 添加账号凭证.
     *
     * @param accountId    账号ID
     * @param identityType 身份类型
     * @param identifier   标识符
     * @param credential   凭证（password 类型会自动 BCrypt 哈希）
     * @param strength     密码强度（可选）
     * @return 凭证ID
     * @throws IllegalArgumentException 当 accountId 或 identifier 为空时
     */
    @Override
    @Transactional
    public Long addCredential(Long accountId, int identityType, String identifier, String credential, Integer strength) {
        if (accountId == null || identifier == null) {
            throw new IllegalArgumentException("accountId / identifier must not be null");
        }
        AuthDO auth = new AuthDO();
        auth.setUserId(accountId);
        auth.setIdentityType(identityType);
        auth.setIdentifier(identifier);
        if (identityType == 1 && credential != null) {
            auth.setCredential(BCrypt.hashpw(credential, BCrypt.gensalt()));
        } else {
            auth.setCredential(credential);
        }
        auth.setCredentialStrength(strength);
        auth.setStatus(1);
        auth.setCreatedAt(LocalDateTime.now());
        auth.setUpdatedAt(LocalDateTime.now());
        authDbService.insert(auth);
        return auth.getId();
    }

    /**
     * 移除账号凭证.
     *
     * @param accountId    账号ID
     * @param identityType 身份类型
     * @param identifier   标识符
     * @return true 移除成功，false 移除失败
     */
    @Override
    @Transactional
    public boolean removeCredential(Long accountId, int identityType, String identifier) {
        return authDbService.deleteByAccountAndIdentity(accountId, identityType, identifier) > 0;
    }
}
