package com.zifang.ctc.core.domain.service;

import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.AuthDO;

/**
 * 凭证域 DbService.
 * <p>
 * 负责 Auth 表 + 联合查询所需的 Account 表读取, 供 core.service.AuthService 使用.
 */
public interface AuthDbService {

    AuthDO selectByIdentity(int identityType, String identifier);

    int insert(AuthDO entity);

    int deleteByAccountAndIdentity(Long accountId, int identityType, String identifier);

    // === 联合查询需要 account ===

    AccountDO selectAccountById(Long id);
}
