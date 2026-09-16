package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.DomainDO;
import com.zifang.ctc.core.domain.service.DomainDbService;
import com.zifang.ctc.core.service.DomainService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 域服务实现类.
 * <p>
 * 业务语义（域编码唯一性校验 + 状态控制） + 事务,
 * 持久化委托给 {@link DomainDbService}.
 *
 * @author zifang
 * @since 1.0.0
 * @see DomainService
 * @see DomainDbService
 */
@Service
public class CtcDomainServiceImpl implements DomainService {

    private static final Logger log = LogManager.getLogger(CtcDomainServiceImpl.class);

    private final DomainDbService domainDbService;

    public CtcDomainServiceImpl(DomainDbService domainDbService) {
        this.domainDbService = domainDbService;
    }

    /**
     * 查询租户下的所有域.
     *
     * @param tenantCode 租户编码
     * @return 域列表
     */
    @Override
    public List<DomainDO> listByTenant(String tenantCode) {
        if (tenantCode == null) { return Collections.emptyList(); }

        return domainDbService.selectByTenant(tenantCode);
    }

    /**
     * 查询所有域.
     *
     * @return 域列表
     */
    @Override
    public List<DomainDO> listAll() {
        return domainDbService.selectAll();
    }

    /**
     * 创建域.
     *
     * @param domain    域信息，必须包含 tenantCode 和 domainCode
     * @param createdBy 创建人
     * @return 域ID
     * @throws IllegalArgumentException 当 domain 为空或必填字段缺失时
     * @throws IllegalStateException    当 domain_code 在 tenant 下已存在时
     */
    @Override
    @Transactional
    public Long createDomain(DomainDO domain, String createdBy) {
        if (domain == null || domain.getDomainCode() == null || domain.getTenantCode() == null) {
            throw new IllegalArgumentException("domainCode / tenantCode 必填");
        }
        DomainDO exist = domainDbService.selectByDomainCode(domain.getTenantCode(), domain.getDomainCode());
        if (exist != null) {
            throw new IllegalStateException(
                    "domain_code=" + domain.getDomainCode() + " 在 tenant=" + domain.getTenantCode() + " 下已存在");
        }
        if (domain.getStatus() == null) domain.setStatus(1);

        domain.setCreatedAt(LocalDateTime.now());
        domain.setUpdatedAt(LocalDateTime.now());
        domainDbService.insert(domain);
        log.info("CtcDomainServiceImpl.createDomain: tenant={} domainCode={} id={}",
                domain.getTenantCode(), domain.getDomainCode(), domain.getId());
        return domain.getId();
    }

    /**
     * 更新域信息.
     *
     * @param id        域ID
     * @param patch     更新内容
     * @param updatedBy 更新人
     * @return true 更新成功，false 域不存在或更新失败
     */
    @Override
    @Transactional
    public boolean updateDomain(Long id, DomainDO patch, String updatedBy) {
        if (id == null || patch == null) { return false; }

        DomainDO exist = domainDbService.selectById(id);
        if (exist == null) { return false; }

        if (patch.getDomainName() != null) exist.setDomainName(patch.getDomainName());

        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());

        if (patch.getDescription() != null) exist.setDescription(patch.getDescription());

        exist.setUpdatedAt(LocalDateTime.now());
        return domainDbService.updateById(exist) > 0;
    }

    /**
     * 删除域.
     *
     * @param id 域ID
     * @return true 删除成功，false 域不存在或删除失败
     */
    @Override
    @Transactional
    public boolean deleteDomain(Long id) {
        if (id == null) { return false; }

        return domainDbService.deleteById(id) > 0;
    }

    /**
     * 根据ID查询域.
     *
     * @param id 域ID
     * @return 域信息，不存在则返回空
     */
    @Override
    public Optional<DomainDO> findById(Long id) {
        return Optional.ofNullable(domainDbService.selectById(id));
    }
}
