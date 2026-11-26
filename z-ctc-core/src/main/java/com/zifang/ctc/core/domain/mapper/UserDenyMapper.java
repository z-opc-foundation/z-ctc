package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.UserDenyDO;

import java.util.List;

/**
 * 用户直接拒绝 Mapper (黑名单).
 */
public interface UserDenyMapper extends BaseMapper<UserDenyDO> {

    /**
     * 按 user_id 查全部 (未过期的).
     */
    default List<UserDenyDO> selectByUserId(Long userId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDenyDO>()
                .eq("user_id", userId)
                .and(w -> w.isNull("expire_at").or().ge("expire_at", java.time.LocalDateTime.now())));
    }

    /**
     * 判重 (user_id + resource_id).
     */
    default UserDenyDO selectByUserAndResource(Long userId, Long resourceId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDenyDO>()
                .eq("user_id", userId)
                .eq("resource_id", resourceId)
                .last("LIMIT 1"));
    }

    /**
     * 按 user_id + resource_id 删.
     */
    default int deleteByUserAndResource(Long userId, Long resourceId) {
        return delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDenyDO>()
                .eq("user_id", userId)
                .eq("resource_id", resourceId));
    }

    /**
     * 按 user_id 删全部 (用户删除时级联清理).
     */
    default int deleteByUserId(Long userId) {
        return delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDenyDO>()
                .eq("user_id", userId));
    }
}
