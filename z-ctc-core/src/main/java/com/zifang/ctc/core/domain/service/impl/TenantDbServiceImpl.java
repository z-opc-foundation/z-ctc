package com.zifang.ctc.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.ctc.core.domain.entity.TenantDO;
import com.zifang.ctc.core.domain.mapper.TenantMapper;
import com.zifang.ctc.core.domain.service.TenantDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TenantDbService 默认实现：直接转发到 {@link TenantMapper}，不加任何业务语义。
 *
 * @author zifang
 * @since 1.0.0
 * @see TenantDbService
 */
@Service
public class TenantDbServiceImpl implements TenantDbService {

    private final TenantMapper tenantMapper;

    /**
     * 构造函数。
     *
     * @param tenantMapper 租户数据访问对象
     */
    public TenantDbServiceImpl(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    /**
     * 插入租户记录。
     *
     * @param entity 租户实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insert(TenantDO entity) {
        return tenantMapper.insert(entity);
    }

    /**
     * 根据主键更新租户记录。
     *
     * @param entity 租户实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateById(TenantDO entity) {
        return tenantMapper.updateById(entity);
    }

    /**
     * 根据主键删除租户记录。
     *
     * @param id 租户主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteById(Long id) {
        return tenantMapper.deleteById(id);
    }

    /**
     * 根据租户编码查询租户记录。
     *
     * @param tenantCode 租户编码
     * @return 租户实体对象，不存在则返回 null
     */
    @Override
    public TenantDO selectByTenantCode(String tenantCode) {
        return tenantMapper.selectByTenantCode(tenantCode);
    }

    /**
     * 根据查询条件查询租户列表。
     *
     * @param wrapper 查询条件包装器
     * @return 租户实体对象列表
     */
    @Override
    public List<TenantDO> selectByQuery(QueryWrapper<TenantDO> wrapper) {
        return tenantMapper.selectList(wrapper);
    }

    /**
     * 分页查询租户列表。
     *
     * @param page    分页对象
     * @param wrapper 查询条件包装器
     * @return 分页的租户实体对象列表
     */
    @Override
    public IPage<TenantDO> selectPage(IPage<TenantDO> page, QueryWrapper<TenantDO> wrapper) {
        return tenantMapper.selectPage(page, wrapper);
    }
}
