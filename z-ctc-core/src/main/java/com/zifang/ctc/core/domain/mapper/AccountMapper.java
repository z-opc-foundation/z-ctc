package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.AccountDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 账号 Mapper.
 */
public interface AccountMapper extends BaseMapper<AccountDO> {

    @Update("UPDATE z_ctc_ac_account SET last_login_at = NOW(), last_login_ip = #{ip} WHERE id = #{id}")
    int updateLastLogin(@Param("id") Long id, @Param("ip") String ip);

    default AccountDO selectByUsername(String username, String tenantCode) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AccountDO>()
                .eq("username", username)
                .eq(tenantCode != null, "tenant_code", tenantCode)
                .last("LIMIT 1"));
    }

    default AccountDO selectByAccountNo(String accountNo) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AccountDO>()
                .eq("account_no", accountNo)
                .last("LIMIT 1"));
    }
}
