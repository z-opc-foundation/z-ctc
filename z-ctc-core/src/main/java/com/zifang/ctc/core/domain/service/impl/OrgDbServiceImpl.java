package com.zifang.ctc.core.domain.service.impl;

import com.zifang.ctc.core.domain.entity.OrgDO;
import com.zifang.ctc.core.domain.mapper.OrgMapper;
import com.zifang.ctc.core.domain.service.OrgDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OrgDbService 默认实现：直接转发到 {@link OrgMapper}，不加任何业务语义。
 *
 * @author zifang
 * @since 1.0.0
 * @see OrgDbService
 */
@Service
public class OrgDbServiceImpl implements OrgDbService {

    private final OrgMapper orgMapper;

    /**
     * 构造函数。
     *
     * @param orgMapper 组织数据访问对象
     */
    public OrgDbServiceImpl(OrgMapper orgMapper) {
        this.orgMapper = orgMapper;
    }

    /**
     * 插入组织记录。
     *
     * @param entity 组织实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insert(OrgDO entity) {
        return orgMapper.insert(entity);
    }

    /**
     * 根据主键更新组织记录。
     *
     * @param entity 组织实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateById(OrgDO entity) {
        return orgMapper.updateById(entity);
    }

    /**
     * 根据主键删除组织记录。
     *
     * @param id 组织主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteById(Long id) {
        return orgMapper.deleteById(id);
    }

    /**
     * 根据租户编码、域编码和组织编码查询组织记录。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param orgCode    组织编码
     * @return 组织实体对象，不存在则返回 null
     */
    @Override
    public OrgDO selectByOrgCode(String tenantCode, String domainCode, String orgCode) {
        return orgMapper.selectByOrgCode(tenantCode, domainCode, orgCode);
    }

    /**
     * 根据租户编码和域编码查询组织列表。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 组织实体对象列表
     */
    @Override
    public List<OrgDO> selectByDomainCode(String tenantCode, String domainCode) {
        return orgMapper.selectByDomainCode(tenantCode, domainCode);
    }
}
