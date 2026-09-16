package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.OrgDO;
import com.zifang.ctc.core.domain.service.OrgDbService;
import com.zifang.ctc.core.service.OrgService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 组织服务实现类.
 * <p>
 * 业务语义（租户/域/组织编码校验 + 状态控制） + 事务,
 * 持久化委托给 {@link OrgDbService}.
 *
 * @author zifang
 * @since 1.0.0
 * @see OrgService
 * @see OrgDbService
 */
@Service
public class AcOrgServiceImpl implements OrgService {

    private static final Logger log = LogManager.getLogger(AcOrgServiceImpl.class);

    private final OrgDbService orgDbService;

    public AcOrgServiceImpl(OrgDbService orgDbService) {
        this.orgDbService = orgDbService;
    }

    /**
     * 创建组织.
     *
     * @param org        组织信息，必须包含 tenantCode、domainCode、orgCode、orgName
     * @param createdBy  创建人
     * @return 组织ID
     * @throws IllegalArgumentException 当 org 为空或必填字段缺失时
     * @throws IllegalStateException    当组织编码已存在时
     */
    @Override
    @Transactional
    public Long createOrg(OrgDO org, String createdBy) {
        if (org == null) { throw new IllegalArgumentException("org 不能为空"); }

        if (org.getTenantCode() == null || org.getDomainCode() == null || org.getOrgCode() == null) {
            throw new IllegalArgumentException("tenantCode / domainCode / orgCode 必填");
        }
        if (org.getOrgName() == null) throw new IllegalArgumentException("orgName 必填");


        OrgDO exist = orgDbService.selectByOrgCode(org.getTenantCode(), org.getDomainCode(), org.getOrgCode());
        if (exist != null) {
            throw new IllegalStateException("组织编码已存在: " + org.getOrgCode());
        }

        if (org.getStatus() == null) org.setStatus(1);

        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        org.setCreatedBy(createdBy);
        org.setUpdatedBy(createdBy);
        if (org.getExtConfig() == null) org.setExtConfig("[]");


        orgDbService.insert(org);
        log.info("创建组织: tenant={}, domain={}, orgCode={}, id={}",
                org.getTenantCode(), org.getDomainCode(), org.getOrgCode(), org.getId());
        return org.getId();
    }

    /**
     * 根据租户/域/组织编码查询组织.
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param orgCode    组织编码
     * @return 组织信息，不存在则返回空
     */
    @Override
    public Optional<OrgDO> findByCode(String tenantCode, String domainCode, String orgCode) {
        return Optional.ofNullable(orgDbService.selectByOrgCode(tenantCode, domainCode, orgCode));
    }

    /**
     * 查询域下的所有组织.
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 组织列表
     */
    @Override
    public List<OrgDO> listByDomain(String tenantCode, String domainCode) {
        if (tenantCode == null || domainCode == null) { return Collections.emptyList(); }

        return orgDbService.selectByDomainCode(tenantCode, domainCode);
    }

    /**
     * 更新组织信息.
     *
     * @param tenantCode  租户编码
     * @param domainCode  域编码
     * @param orgCode     组织编码
     * @param patch       更新内容
     * @param updatedBy   更新人
     * @return true 更新成功，false 更新失败或组织不存在
     */
    @Override
    @Transactional
    public boolean updateOrg(String tenantCode, String domainCode, String orgCode, OrgDO patch, String updatedBy) {
        if (tenantCode == null || domainCode == null || orgCode == null || patch == null) { return false; }

        OrgDO exist = orgDbService.selectByOrgCode(tenantCode, domainCode, orgCode);
        if (exist == null) { return false; }

        if (patch.getOrgName() != null) exist.setOrgName(patch.getOrgName());

        if (patch.getDescription() != null) exist.setDescription(patch.getDescription());

        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());

        if (patch.getExtConfig() != null) exist.setExtConfig(patch.getExtConfig());

        exist.setUpdatedAt(LocalDateTime.now());
        exist.setUpdatedBy(updatedBy);
        return orgDbService.updateById(exist) > 0;
    }

    /**
     * 删除组织.
     *
     * @param tenantCode  租户编码
     * @param domainCode  域编码
     * @param orgCode     组织编码
     * @return true 删除成功，false 组织不存在或删除失败
     */
    @Override
    @Transactional
    public boolean deleteOrg(String tenantCode, String domainCode, String orgCode) {
        OrgDO exist = orgDbService.selectByOrgCode(tenantCode, domainCode, orgCode);
        if (exist == null) { return false; }

        return orgDbService.deleteById(exist.getId()) > 0;
    }
}
