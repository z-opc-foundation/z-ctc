package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.AuthDO;

/**
 * 凭证 Mapper.
 */
public interface AuthMapper extends BaseMapper<AuthDO> {

    default AuthDO selectByIdentity(int identityType, String identifier) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AuthDO>()
                .eq("identity_type", identityType)
                .eq("identifier", identifier)
                .last("LIMIT 1"));
    }

    default java.util.List<AuthDO> selectByAccountId(Long accountId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AuthDO>()
                .eq("user_id", accountId));
    }
}
