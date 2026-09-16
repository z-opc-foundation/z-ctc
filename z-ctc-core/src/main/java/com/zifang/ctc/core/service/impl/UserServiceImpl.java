package com.zifang.ctc.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.AuthDO;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.mapper.AuthMapper;
import com.zifang.ctc.core.domain.service.UserDbService;
import com.zifang.ctc.core.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务实现类.
 * <p>
 * BCrypt 密码哈希, 状态控制, 业务校验, 持久化委托给 {@link UserDbService}.
 * Auth (凭证) 表直接用 AuthMapper, 暂不抽 AuthDbService (业务简单).
 *
 * @author zifang
 * @since 1.0.0
 * @see UserService
 * @see UserDbService
 * @see AuthMapper
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LogManager.getLogger(UserServiceImpl.class);

    private final UserDbService userDbService;
    private final AuthMapper authMapper;

    public UserServiceImpl(UserDbService userDbService, AuthMapper authMapper) {
        this.userDbService = userDbService;
        this.authMapper = authMapper;
    }

    /**
     * 注册用户（邮箱 + 密码）.
     *
     * @param email     邮箱（必须唯一）
     * @param username  用户名（可选，为空则取邮箱前缀）
     * @param nickname  昵称（可选，为空则取用户名）
     * @param rawPassword 原始密码（至少 6 位）
     * @return 用户ID
     * @throws IllegalArgumentException 当 email 或 password 不合法时
     * @throws IllegalStateException    当邮箱已被注册时
     */
    @Override
    @Transactional
    public Long register(String email, String username, String nickname, String rawPassword) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("email 必填");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (userDbService.selectByEmail(email) != null) {
            throw new IllegalStateException("邮箱已被注册");
        }

        UserDO user = new UserDO();
        user.setUserNo("U" + System.currentTimeMillis());
        user.setEmail(email);
        user.setUsername(username != null ? username : email.split("@")[0]);
        user.setNickname(nickname != null ? nickname : user.getUsername());
        user.setStatus(1);
        user.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userDbService.insert(user);

        // 建密码凭证
        AuthDO auth = new AuthDO();
        auth.setUserId(user.getId());
        auth.setIdentityType(1);  // 1=密码
        auth.setIdentifier(email);
        auth.setCredential(user.getPasswordHash());
        auth.setStatus(1);
        auth.setCreatedAt(LocalDateTime.now());
        auth.setUpdatedAt(LocalDateTime.now());
        authMapper.insert(auth);

        log.info("UserServiceImpl.register 成功: userId={}, email={}", user.getId(), email);
        return user.getId();
    }

    /**
     * 注册用户（手机号 + 密码）.
     *
     * @param phone     手机号（必须唯一）
     * @param username  用户名（可选，为空则取 phone_前缀）
     * @param nickname  昵称（可选，为空则取用户名）
     * @param rawPassword 原始密码（至少 6 位）
     * @return 用户ID
     * @throws IllegalArgumentException 当 phone 或 password 不合法时
     * @throws IllegalStateException    当手机号已被注册时
     */
    @Override
    @Transactional
    public Long registerByPhone(String phone, String username, String nickname, String rawPassword) {
        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("phone 必填");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (userDbService.selectByPhone(phone) != null) {
            throw new IllegalStateException("该手机号已注册");
        }

        UserDO user = new UserDO();
        user.setUserNo("U" + System.currentTimeMillis());
        user.setPhone(phone);
        user.setUsername(username != null && !username.isEmpty() ? username : "phone_" + phone);
        user.setNickname(nickname != null && !nickname.isEmpty() ? nickname : user.getUsername());
        user.setStatus(1);
        user.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userDbService.insert(user);

        // 建密码凭证: identifier=phone, 支持手机号+密码登录 (login 端点 identityType=1)
        AuthDO auth = new AuthDO();
        auth.setUserId(user.getId());
        auth.setIdentityType(1);  // 1=密码
        auth.setIdentifier(phone);
        auth.setCredential(user.getPasswordHash());
        auth.setStatus(1);
        auth.setCreatedAt(LocalDateTime.now());
        auth.setUpdatedAt(LocalDateTime.now());
        authMapper.insert(auth);

        log.info("UserServiceImpl.registerByPhone 成功: userId={}, phone={}", user.getId(), phone);
        return user.getId();
    }

    /**
     * 根据ID查询用户.
     *
     * @param id 用户ID
     * @return 用户信息，不存在则返回空
     */
    @Override
    public Optional<UserDO> findById(Long id) {
        if (id == null) { return Optional.empty(); }

        return Optional.ofNullable(userDbService.selectById(id));
    }

    /**
     * 根据邮箱查询用户.
     *
     * @param email 邮箱
     * @return 用户信息，不存在则返回空
     */
    @Override
    public Optional<UserDO> findByEmail(String email) {
        if (email == null) { return Optional.empty(); }

        return Optional.ofNullable(userDbService.selectByEmail(email));
    }

    /**
     * 根据手机号查询用户.
     *
     * @param phone 手机号
     * @return 用户信息，不存在则返回空
     */
    @Override
    public Optional<UserDO> findByPhone(String phone) {
        if (phone == null) { return Optional.empty(); }

        return Optional.ofNullable(userDbService.selectByPhone(phone));
    }

    /**
     * 根据用户名查询用户.
     *
     * @param username 用户名
     * @return 用户信息，不存在则返回空
     */
    @Override
    public Optional<UserDO> findByUsername(String username) {
        return Optional.ofNullable(userDbService.selectByUsername(username));
    }

    /**
     * 根据身份类型和标识符查询用户.
     * <p>
     * identityType=1 时按 email 查询，identityType=2 时按 phone 查询，其他按 username 查询.
     *
     * @param identityType 身份类型
     * @param identifier   标识符
     * @return 用户信息，不存在则返回空
     */
    @Override
    public Optional<UserDO> findByLoginIdentifier(int identityType, String identifier) {
        if (identifier == null) { return Optional.empty(); }

        if (identityType == 1) {
            return findByEmail(identifier);
        } else if (identityType == 2) {
            return findByPhone(identifier);
        } else {
            return findByUsername(identifier);
        }
    }

    /**
     * 更新用户最后登录时间和 IP.
     *
     * @param userId 用户ID
     * @param ip     登录 IP
     * @return true 更新成功，false 用户不存在或更新失败
     */
    @Override
    public boolean updateLastLogin(Long userId, String ip) {
        UserDO user = userDbService.selectById(userId);
        if (user == null) { return false; }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        user.setUpdatedAt(LocalDateTime.now());
        return userDbService.updateById(user) > 0;
    }

    /**
     * 更新用户状态.
     *
     * @param userId    用户ID
     * @param newStatus 新状态
     * @return true 更新成功，false 用户不存在或更新失败
     */
    @Override
    @Transactional
    public boolean updateStatus(Long userId, int newStatus) {
        UserDO user = userDbService.selectById(userId);
        if (user == null) { return false; }

        user.setStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        return userDbService.updateById(user) > 0;
    }

    /**
     * 修改密码（需验证旧密码）.
     *
     * @param userId          用户ID
     * @param oldRawPassword  旧密码
     * @param newRawPassword  新密码
     * @return true 修改成功，false 用户不存在、旧密码错误或修改失败
     */
    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldRawPassword, String newRawPassword) {
        UserDO user = userDbService.selectById(userId);
        if (user == null || user.getPasswordHash() == null) return false;

        if (!BCrypt.checkpw(oldRawPassword, user.getPasswordHash())) {
            log.warn("UserServiceImpl.changePassword 旧密码错误: userId={}", userId);
            return false;
        }
        user.setPasswordHash(BCrypt.hashpw(newRawPassword, BCrypt.gensalt()));
        user.setUpdatedAt(LocalDateTime.now());
        boolean ok = userDbService.updateById(user) > 0;
        if (ok) {
            // 同步更新凭证表的 credential
            AuthDO auth = authMapper.selectByIdentity(1, user.getEmail());
            if (auth != null) {
                auth.setCredential(user.getPasswordHash());
                auth.setUpdatedAt(LocalDateTime.now());
                authMapper.updateById(auth);
            }
        }
        return ok;
    }

    /**
     * 重置密码（无需验证旧密码）.
     *
     * @param userId         用户ID
     * @param newRawPassword 新密码（至少 6 位）
     * @return true 重置成功，false 用户不存在或重置失败
     * @throws IllegalArgumentException 当新密码不合法时
     */
    @Override
    @Transactional
    public boolean resetPassword(Long userId, String newRawPassword) {
        UserDO user = userDbService.selectById(userId);
        if (user == null) { return false; }

        if (newRawPassword == null || newRawPassword.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        user.setPasswordHash(BCrypt.hashpw(newRawPassword, BCrypt.gensalt()));
        user.setUpdatedAt(LocalDateTime.now());
        boolean ok = userDbService.updateById(user) > 0;
        if (ok) {
            // 同步凭证表 credential: 邮箱注册用户按 email 查, 手机注册用户按 phone 查
            AuthDO auth = null;
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                auth = authMapper.selectByIdentity(1, user.getEmail());
            }
            if (auth == null && user.getPhone() != null && !user.getPhone().isEmpty()) {
                auth = authMapper.selectByIdentity(1, user.getPhone());
            }
            if (auth != null) {
                auth.setCredential(user.getPasswordHash());
                auth.setUpdatedAt(LocalDateTime.now());
                authMapper.updateById(auth);
            }
        }
        return ok;
    }

    /**
     * 验证密码是否匹配.
     *
     * @param rawPassword  原始密码
     * @param passwordHash 哈希值
     * @return true 密码匹配，false 不匹配或异常
     */
    @Override
    public boolean verifyPassword(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) { return false; }

        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (Exception e) {
            log.warn("UserServiceImpl.verifyPassword 失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据 ID 列表查询用户.
     *
     * @param ids 用户 ID 列表
     * @return 用户列表
     */
    @Override
    public List<UserDO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) { return Collections.emptyList(); }
        return userDbService.selectByQuery(new QueryWrapper<UserDO>().in("id", ids));
    }
}
