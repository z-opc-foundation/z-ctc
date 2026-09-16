package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.UserGrantDO;

import java.util.List;

/**
 * 用户直接授权 Mapper (白名单).
 */
public interface UserGrantMapper extends BaseMapper<UserGrantDO> {

    /**
     * 按 user_id 查全部 (未过期的).
     */
    default List<UserGrantDO> selectByUserId(Long userId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserGrantDO>()
                .eq("user_id", userId)
                .and(w -> w.isNull("expire_at").or().ge("expire_at", java.time.LocalDateTime.now())));
    }

    /**
     * 判重 (user_id + resource_id).
     */
    default UserGrantDO selectByUserAndResource(Long userId, Long resourceId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserGrantDO>()
                .eq("user_id", userId)
                .eq("resource_id", resourceId)
                .last("LIMIT 1"));
    }

    /**
     * 按 user_id + resource_id 删 (反向授权).
     */
    default int deleteByUserAndResource(Long userId, Long resourceId) {
        return delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserGrantDO>()
                .eq("user_id", userId)
                .eq("resource_id", resourceId));
    }

    /**
     * 按 user_id 删全部 (用户删除时级联清理).
     */
    default int deleteByUserId(Long userId) {
        return delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserGrantDO>()
                .eq("user_id", userId));
    }

    /**
     * 列出持有指定资源授权的用户 ID.
     */
    default List<Long> selectUserIdsByResourceId(Long resourceId) {
        return selectObjs(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserGrantDO>()
                .eq("resource_id", resourceId)
                .select("user_id"));
    }
}
