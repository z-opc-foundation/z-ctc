package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.InvitationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * z_ctc_ac_invitation Mapper (FEATURE049 邀请记录).
 */
@Mapper
public interface InvitationMapper extends BaseMapper<InvitationDO> {

    default InvitationDO selectByToken(String token) {
        if (token == null) { return null; }

        return selectOne(new QueryWrapper<InvitationDO>()
                .eq("token", token)
                .last("LIMIT 1"));
    }

    default InvitationDO selectByInviteCode(String code) {
        if (code == null) { return null; }

        return selectOne(new QueryWrapper<InvitationDO>()
                .eq("invite_code", code)
                .last("LIMIT 1"));
    }

    default List<InvitationDO> selectByTenant(String tenantCode) {
        return selectList(new QueryWrapper<InvitationDO>()
                .eq("tenant_code", tenantCode)
                .orderByDesc("id"));
    }

    default List<InvitationDO> selectByInviteeEmail(String email) {
        if (email == null) { return null; }

        return selectList(new QueryWrapper<InvitationDO>()
                .eq("invitee_email", email)
                .eq("status", 0)
                .orderByDesc("id"));
    }
}
