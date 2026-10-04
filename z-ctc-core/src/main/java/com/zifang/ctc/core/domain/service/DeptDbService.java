package com.zifang.ctc.core.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.DeptDO;

import java.util.List;

/**
 * 部门域 DbService.
 */
public interface DeptDbService {

    int insert(DeptDO entity);

    int updateById(DeptDO entity);

    int deleteById(Long id);

    DeptDO selectByDeptCode(String tenantCode, String domainCode, String deptCode);

    List<DeptDO> selectByOrgCode(String tenantCode, String domainCode, String orgCode);

    /**
     * 按条件查询部门列表.
     * <p>2026-10-04 新增：为部门树（{@code AcDeptTreeServiceImpl}）提供
     * "按 (租户, 域) 取全量部门" 的能力——既有方法都要求 orgCode，
     * 而树形消费方需要跨组织的该域全量部门。
     * <p>约定与 {@code UserDbService} / {@code MembershipDbService} 的同名方法一致。
     *
     * @param wrapper MyBatis-Plus 查询条件
     * @return 匹配的部门列表（永不为 null）
     */
    List<DeptDO> selectByQuery(QueryWrapper<DeptDO> wrapper);
}
