package com.zifang.ctc.core.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.InvitationDO;

import java.util.List;

/**
 * z_ctc_ac_invitation 域 DbService (FEATURE049).
 * <p>
 * 邀请记录的纯 mapper 包装.
 */
public interface InvitationDbService {

    int insert(InvitationDO entity);

    int updateById(InvitationDO entity);

    int deleteById(Long id);

    InvitationDO selectById(Long id);

    InvitationDO selectByToken(String token);

    InvitationDO selectByInviteCode(String code);

    List<InvitationDO> selectByTenant(String tenantCode);

    List<InvitationDO> selectByInviteeEmail(String email);

    List<InvitationDO> selectByQuery(QueryWrapper<InvitationDO> wrapper);
}
