package com.zifang.ctc.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.domain.entity.AuthDO;
import com.zifang.ctc.core.domain.mapper.AccountMapper;
import com.zifang.ctc.core.domain.mapper.AuthMapper;
import com.zifang.ctc.core.domain.service.AuthDbService;
import org.springframework.stereotype.Service;

/**
 * AuthDbService 默认实现：直接转发到对应 Mapper，不加任何业务语义。
 *
 * @author zifang
 * @since 1.0.0
 * @see AuthDbService
 */
@Service
public class AuthDbServiceImpl implements AuthDbService {

    private final AuthMapper authMapper;
    private final AccountMapper accountMapper;

    /**
     * 构造函数。
     *
     * @param authMapper    认证数据访问对象
     * @param accountMapper 账号数据访问对象
     */
    public AuthDbServiceImpl(AuthMapper authMapper, AccountMapper accountMapper) {
        this.authMapper = authMapper;
        this.accountMapper = accountMapper;
    }

    /**
     * 根据身份类型和标识符查询认证记录。
     *
     * @param identityType 身份类型
     * @param identifier   标识符
     * @return 认证实体对象，不存在则返回 null
     */
    @Override
    public AuthDO selectByIdentity(int identityType, String identifier) {
        return authMapper.selectByIdentity(identityType, identifier);
    }

    /**
     * 插入认证记录。
     *
     * @param entity 认证实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insert(AuthDO entity) {
        return authMapper.insert(entity);
    }

    /**
     * 根据账号主键和身份信息删除认证记录。
     *
     * @param accountId     账号主键
     * @param identityType  身份类型
     * @param identifier    标识符
     * @return 删除成功的记录数
     */
    @Override
    public int deleteByAccountAndIdentity(Long accountId, int identityType, String identifier) {
        return authMapper.delete(new QueryWrapper<AuthDO>()
                .eq("user_id", accountId)
                .eq("identity_type", identityType)
                .eq("identifier", identifier));
    }

    /**
     * 根据主键查询账号记录。
     *
     * @param id 账号主键
     * @return 账号实体对象，不存在则返回 null
     */
    @Override
    public AccountDO selectAccountById(Long id) {
        return accountMapper.selectById(id);
    }
}
