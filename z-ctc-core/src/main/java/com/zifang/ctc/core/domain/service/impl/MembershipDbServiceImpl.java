package com.zifang.ctc.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.mapper.MembershipMapper;
import com.zifang.ctc.core.domain.service.MembershipDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MembershipDbService 默认实现：转发到 {@link MembershipMapper}。
 *
 * @author zifang
 * @since 1.0.0
 * @see MembershipDbService
 */
@Service
public class MembershipDbServiceImpl implements MembershipDbService {

    private final MembershipMapper membershipMapper;

    /**
     * 构造函数。
     *
     * @param membershipMapper 会员数据访问对象
     */
    public MembershipDbServiceImpl(MembershipMapper membershipMapper) {
        this.membershipMapper = membershipMapper;
    }

    /**
     * 插入会员关系记录。
     *
     * @param entity 会员关系实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insert(MembershipDO entity) {
        return membershipMapper.insert(entity);
    }

    /**
     * 根据主键更新会员关系记录。
     *
     * @param entity 会员关系实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateById(MembershipDO entity) {
        return membershipMapper.updateById(entity);
    }

    /**
     * 根据主键删除会员关系记录。
     *
     * @param id 会员关系主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteById(Long id) {
        return membershipMapper.deleteById(id);
    }

    /**
     * 根据主键查询会员关系记录。
     *
     * @param id 会员关系主键
     * @return 会员关系实体对象，不存在则返回 null
     */
    @Override
    public MembershipDO selectById(Long id) {
        return membershipMapper.selectById(id);
    }

    /**
     * 根据用户主键和租户编码查询会员关系记录。
     *
     * @param userId     用户主键
     * @param tenantCode 租户编码
     * @return 会员关系实体对象，不存在则返回 null
     */
    @Override
    public MembershipDO selectByUserAndTenant(Long userId, String tenantCode) {
        return membershipMapper.selectByUserAndTenant(userId, tenantCode);
    }

    /**
     * 查询用户的活跃会员关系列表。
     *
     * @param userId 用户主键
     * @return 会员关系实体对象列表
     */
    @Override
    public List<MembershipDO> selectActiveByUserId(Long userId) {
        return membershipMapper.selectActiveByUserId(userId);
    }

    /**
     * 查询用户的所有会员关系列表。
     *
     * @param userId 用户主键
     * @return 会员关系实体对象列表
     */
    @Override
    public List<MembershipDO> selectAllByUserId(Long userId) {
        return membershipMapper.selectAllByUserId(userId);
    }

    /**
     * 查询租户的活跃会员关系列表。
     *
     * @param tenantCode 租户编码
     * @return 会员关系实体对象列表
     */
    @Override
    public List<MembershipDO> selectActiveByTenant(String tenantCode) {
        return membershipMapper.selectActiveByTenant(tenantCode);
    }

    /**
     * 查询用户活跃会员关系数量。
     *
     * @param userId 用户主键
     * @return 数量
     */
    @Override
    public long countActiveByUserId(Long userId) {
        Long cnt = membershipMapper.countActiveByUserId(userId);
        return cnt == null ? 0L : cnt;
    }

    /**
     * 根据查询条件查询会员关系列表。
     *
     * @param wrapper 查询条件包装器
     * @return 会员关系实体对象列表
     */
    @Override
    public List<MembershipDO> selectByQuery(QueryWrapper<MembershipDO> wrapper) {
        return membershipMapper.selectList(wrapper);
    }
}
