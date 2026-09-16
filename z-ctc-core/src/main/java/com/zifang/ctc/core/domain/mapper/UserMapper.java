package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * z_ctc_ac_user Mapper (FEATURE049 全局用户).
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    default UserDO selectByUserNo(String userNo) {
        return selectOne(new QueryWrapper<UserDO>()
                .eq("user_no", userNo)
                .last("LIMIT 1"));
    }

    default UserDO selectByEmail(String email) {
        if (email == null) { return null; }

        return selectOne(new QueryWrapper<UserDO>()
                .eq("email", email)
                .last("LIMIT 1"));
    }

    default UserDO selectByPhone(String phone) {
        if (phone == null) { return null; }

        return selectOne(new QueryWrapper<UserDO>()
                .eq("phone", phone)
                .last("LIMIT 1"));
    }

    default UserDO selectByLoginIdentifier(int identityType, String identifier) {
        if (identifier == null) { return null; }

        if (identityType == 1) {
            return selectByEmail(identifier);
        } else if (identityType == 2) {
            return selectByPhone(identifier);
        } else {
            // username
            return selectOne(new QueryWrapper<UserDO>()
                    .eq("username", identifier)
                    .last("LIMIT 1"));
        }
    }
}
