package com.zifang.ctc.core.domain.service.impl;

import com.zifang.ctc.core.domain.entity.DomainDO;
import com.zifang.ctc.core.domain.mapper.DomainMapper;
import com.zifang.ctc.core.domain.service.DomainDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DomainDbService 默认实现.
 * <p>
 * 直接转发到 {@link DomainMapper}，不加任何业务语义.
 *
 * @author zifang
 * @since 1.0.0
 * @see DomainDbService
 * @see DomainMapper
 */
@Service
public class DomainDbServiceImpl implements DomainDbService {

    private final DomainMapper domainMapper;

    public DomainDbServiceImpl(DomainMapper domainMapper) {
        this.domainMapper = domainMapper;
    }

    /**
     * 根据租户编码查询域列表.
     *
     * @param tenantCode 租户编码
     * @return 域列表
     */
    @Override
    public List<DomainDO> selectByTenant(String tenantCode) {
        return domainMapper.selectByTenant(tenantCode);
    }

    /**
     * 查询所有域.
     *
     * @return 域列表
     */
    @Override
    public List<DomainDO> selectAll() {
        return domainMapper.selectAll();
    }

    /**
     * 根据ID查询域.
     *
     * @param id 主键ID
     * @return 域对象
     */
    @Override
    public DomainDO selectById(Long id) {
        return domainMapper.selectById(id);
    }

    /**
     * 根据租户/域编码查询域.
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 域对象
     */
    @Override
    public DomainDO selectByDomainCode(String tenantCode, String domainCode) {
        return domainMapper.selectByDomainCode(tenantCode, domainCode);
    }

    /**
     * 插入域.
     *
     * @param entity 域对象
     * @return 影响行数
     */
    @Override
    public int insert(DomainDO entity) {
        return domainMapper.insert(entity);
    }

    /**
     * 根据ID更新域.
     *
     * @param entity 域对象（必须包含 ID）
     * @return 影响行数
     */
    @Override
    public int updateById(DomainDO entity) {
        return domainMapper.updateById(entity);
    }

    /**
     * 根据ID删除域.
     *
     * @param id 主键ID
     * @return 影响行数
     */
    @Override
    public int deleteById(Long id) {
        return domainMapper.deleteById(id);
    }
}
