package com.zifang.ctc.core.domain.service.impl;

import com.zifang.ctc.core.domain.entity.DeptDO;
import com.zifang.ctc.core.domain.mapper.DeptMapper;
import com.zifang.ctc.core.domain.service.DeptDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DeptDbService 默认实现：直接转发到 {@link DeptMapper}，不加任何业务语义。
 *
 * @author zifang
 * @since 1.0.0
 * @see DeptDbService
 */
@Service
public class DeptDbServiceImpl implements DeptDbService {

    private final DeptMapper deptMapper;

    /**
     * 构造函数。
     *
     * @param deptMapper 部门数据访问对象
     */
    public DeptDbServiceImpl(DeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    /**
     * 插入部门记录。
     *
     * @param entity 部门实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insert(DeptDO entity) {
        return deptMapper.insert(entity);
    }

    /**
     * 根据主键更新部门记录。
     *
     * @param entity 部门实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateById(DeptDO entity) {
        return deptMapper.updateById(entity);
    }

    /**
     * 根据主键删除部门记录。
     *
     * @param id 部门主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteById(Long id) {
        return deptMapper.deleteById(id);
    }

    /**
     * 根据租户编码、域编码和部门编码查询部门记录。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param deptCode   部门编码
     * @return 部门实体对象，不存在则返回 null
     */
    @Override
    public DeptDO selectByDeptCode(String tenantCode, String domainCode, String deptCode) {
        return deptMapper.selectByDeptCode(tenantCode, domainCode, deptCode);
    }

    /**
     * 根据租户编码、域编码和组织编码查询部门列表。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param orgCode    组织编码
     * @return 部门实体对象列表
     */
    @Override
    public List<DeptDO> selectByOrgCode(String tenantCode, String domainCode, String orgCode) {
        return deptMapper.selectByOrgCode(tenantCode, domainCode, orgCode);
    }
}
