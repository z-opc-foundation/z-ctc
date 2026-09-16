package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.UserDO;

import java.util.List;
import java.util.Optional;

/**
 * 全局用户服务 (FEATURE049).
 * <p>
 * 身份层业务逻辑. 一个真实的人 = 一个全局 User, 不绑租户.
 * 租户关系由 {@link MembershipService} 管理.
 */
public interface UserService {

    /**
     * 注册新用户 (创建游离 user, 不建 membership).
     *
     * @param email       邮箱 (全局唯一, 登录用)
     * @param username    用户名 (非唯一, 显示用)
     * @param nickname    昵称
     * @param rawPassword 原始密码 (将做 BCrypt 哈希)
     * @return 新建 user id
     */
    Long register(String email, String username, String nickname, String rawPassword);

    /**
     * 手机号注册新用户 (验证码登录/找回密码体系).
     *
     * @param phone       手机号 (全局唯一, 登录用)
     * @param username    用户名 (非唯一, 显示用; 为空时默认 phone_ 前缀)
     * @param nickname    昵称
     * @param rawPassword 原始密码 (将做 BCrypt 哈希)
     * @return 新建 user id
     */
    Long registerByPhone(String phone, String username, String nickname, String rawPassword);
    Optional<UserDO> findById(Long id);

    Optional<UserDO> findByEmail(String email);

    Optional<UserDO> findByPhone(String phone);

    Optional<UserDO> findByUsername(String username);

    Optional<UserDO> findByLoginIdentifier(int identityType, String identifier);

    /**
     * 更新最后登录时间和 IP.
     */
    boolean updateLastLogin(Long userId, String ip);

    /**
     * 修改用户状态 (0=禁用 1=正常 2=锁定).
     */
    boolean updateStatus(Long userId, int newStatus);

    /**
     * 修改密码 (含原密码校验).
     */
    boolean changePassword(Long userId, String oldRawPassword, String newRawPassword);

    /**
     * 重置密码 (管理员操作, 不校验原密码).
     */
    boolean resetPassword(Long userId, String newRawPassword);

    /**
     * BCrypt 校验密码.
     */
    boolean verifyPassword(String rawPassword, String passwordHash);

    /**
     * 业务查询: 按 userId 列表批量拿用户 (membership/invitation 页面用).
     */
    List<UserDO> listByIds(List<Long> ids);
}
