package com.zifang.ctc.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.mapper.UserMapper;
import com.zifang.ctc.core.domain.service.UserDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserDbService 默认实现：转发到 {@link UserMapper}。
 *
 * @author zifang
 * @since 1.0.0
 * @see UserDbService
 */
@Service
public class UserDbServiceImpl implements UserDbService {

    private final UserMapper userMapper;

    /**
     * 构造函数。
     *
     * @param userMapper 用户数据访问对象
     */
    public UserDbServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 插入用户记录。
     *
     * @param entity 用户实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insert(UserDO entity) {
        return userMapper.insert(entity);
    }

    /**
     * 根据主键更新用户记录。
     *
     * @param entity 用户实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateById(UserDO entity) {
        return userMapper.updateById(entity);
    }

    /**
     * 根据主键删除用户记录。
     *
     * @param id 用户主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteById(Long id) {
        return userMapper.deleteById(id);
    }

    /**
     * 根据主键查询用户记录。
     *
     * @param id 用户主键
     * @return 用户实体对象，不存在则返回 null
     */
    @Override
    public UserDO selectById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 根据用户编号查询用户记录。
     *
     * @param userNo 用户编号
     * @return 用户实体对象，不存在则返回 null
     */
    @Override
    public UserDO selectByUserNo(String userNo) {
        return userMapper.selectByUserNo(userNo);
    }

    /**
     * 根据邮箱查询用户记录。
     *
     * @param email 邮箱地址
     * @return 用户实体对象，不存在则返回 null
     */
    @Override
    public UserDO selectByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    /**
     * 根据手机号查询用户记录。
     *
     * @param phone 手机号码
     * @return 用户实体对象，不存在则返回 null
     */
    @Override
    public UserDO selectByPhone(String phone) {
        return userMapper.selectByPhone(phone);
    }

    /**
     * 根据用户名查询用户记录。
     *
     * @param username 用户名
     * @return 用户实体对象，不存在则返回 null
     */
    @Override
    public UserDO selectByUsername(String username) {
        if (username == null) { return null; }

        return userMapper.selectOne(new QueryWrapper<UserDO>()
                .eq("username", username)
                .last("LIMIT 1"));
    }

    /**
     * 根据查询条件查询用户列表。
     *
     * @param wrapper 查询条件包装器
     * @return 用户实体对象列表
     */
    @Override
    public List<UserDO> selectByQuery(QueryWrapper<UserDO> wrapper) {
        return userMapper.selectList(wrapper);
    }
}
