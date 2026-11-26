package com.zifang.ctc.core.service;

import java.util.Optional;

/**
 * 凭证 + 登录服务接口.
 * <p>
 * 登录流程:
 * <ol>
 *   <li>用 AuthService.findAccountByCredential 找到账号 (或 null)</li>
 *   <li>用 AuthService.verifyPassword 校验密码 (账号不存在或密码错都返 false)</li>
 *   <li>用 JwtUtil 发 token (业务方按需 — z-ctc-sso 提供)</li>
 *   <li>用 LoginLogService 异步落日志 (不管成功失败都记)</li>
 * </ol>
 */
public interface AuthService {

    /**
     * 按凭证标识 (用户名/手机号/邮箱) 找账号 + 凭证.
     */
    Optional<AccountWithAuth> findAccountByCredential(int identityType, String identifier, String tenantCode);

    /**
     * 校验密码 (BCrypt). 业务方按 credentialType=1 调.
     */
    boolean verifyPassword(String rawPassword, String storedHash);

    /**
     * 写入新凭证 (一个账号可绑多种凭证).
     */
    Long addCredential(Long accountId, int identityType, String identifier, String credential, Integer strength);

    /**
     * 删除某凭证.
     */
    boolean removeCredential(Long accountId, int identityType, String identifier);
}
