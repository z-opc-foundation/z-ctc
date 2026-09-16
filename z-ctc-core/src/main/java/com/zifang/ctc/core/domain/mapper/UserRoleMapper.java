package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.UserRoleDO;

import java.util.List;

public interface UserRoleMapper extends BaseMapper<UserRoleDO> {

    default List<UserRoleDO> selectByUserId(Long userId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserRoleDO>()
                .eq("user_id", userId)
                .and(w -> w.isNull("expire_at").or().gt("expire_at", java.time.LocalDateTime.now())));
    }
}
