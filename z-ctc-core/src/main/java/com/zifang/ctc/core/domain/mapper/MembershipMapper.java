package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * z_ctc_ac_membership Mapper (FEATURE049 租户成员关系).
 */
@Mapper
public interface MembershipMapper extends BaseMapper<MembershipDO> {

    /**
     * 查 user 在某租户的成员关系
     */
    default MembershipDO selectByUserAndTenant(Long userId, String tenantCode) {
        return selectOne(new QueryWrapper<MembershipDO>()
                .eq("user_id", userId)
                .eq("tenant_code", tenantCode)
                .last("LIMIT 1"));
    }

    /**
     * 查 user 的所有 active 成员关系
     */
    default List<MembershipDO> selectActiveByUserId(Long userId) {
        return selectList(new QueryWrapper<MembershipDO>()
                .eq("user_id", userId)
                .eq("member_status", 1)
                .orderByDesc("id"));
    }

    /**
     * 查 user 的所有成员关系 (含历史)
     */
    default List<MembershipDO> selectAllByUserId(Long userId) {
        return selectList(new QueryWrapper<MembershipDO>()
                .eq("user_id", userId)
                .orderByDesc("id"));
    }

    /**
     * 查某租户的所有 active 成员
     */
    default List<MembershipDO> selectActiveByTenant(String tenantCode) {
        return selectList(new QueryWrapper<MembershipDO>()
                .eq("tenant_code", tenantCode)
                .eq("member_status", 1)
                .orderByDesc("id"));
    }

    /**
     * 查某租户某部门的 active 成员
     */
    default List<MembershipDO> selectActiveByDept(String tenantCode, String deptCode) {
        return selectList(new QueryWrapper<MembershipDO>()
                .eq("tenant_code", tenantCode)
                .eq("dept_code", deptCode)
                .eq("member_status", 1)
                .orderByDesc("id"));
    }

    /**
     * 查某租户一组部门 (如 org 展开的 dept 集合) 的 active 成员
     */
    default List<MembershipDO> selectActiveByDeptCodes(String tenantCode, java.util.Collection<String> deptCodes) {
        return selectList(new QueryWrapper<MembershipDO>()
                .eq("tenant_code", tenantCode)
                .in("dept_code", deptCodes)
                .eq("member_status", 1)
                .orderByDesc("id"));
    }

    /**
     * 查 user 的 active 租户数量
     */
    default Long countActiveByUserId(Long userId) {
        return selectCount(new QueryWrapper<MembershipDO>()
                .eq("user_id", userId)
                .eq("member_status", 1));
    }
}
