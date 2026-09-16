package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.DeptDO;
import com.zifang.ctc.core.domain.service.DeptDbService;
import com.zifang.ctc.core.service.DeptService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 部门服务实现类.
 * <p>
 * 业务语义（租户/域/组织/部门编码校验 + 状态控制） + 事务,
 * 持久化委托给 {@link DeptDbService}.
 *
 * @author zifang
 * @since 1.0.0
 * @see DeptService
 * @see DeptDbService
 */
@Service
public class AcDeptServiceImpl implements DeptService {

    private static final Logger log = LogManager.getLogger(AcDeptServiceImpl.class);

    private final DeptDbService deptDbService;

    public AcDeptServiceImpl(DeptDbService deptDbService) {
        this.deptDbService = deptDbService;
    }

    /**
     * 创建部门.
     *
     * @param dept      部门信息，必须包含 tenantCode、domainCode、orgCode、deptCode、deptName
     * @param createdBy 创建人
     * @return 部门ID
     * @throws IllegalArgumentException 当 dept 为空或必填字段缺失时
     * @throws IllegalStateException    当部门编码已存在时
     */
    @Override
    @Transactional
    public Long createDept(DeptDO dept, String createdBy) {
        if (dept == null) { throw new IllegalArgumentException("dept 不能为空"); }

        if (dept.getTenantCode() == null || dept.getDomainCode() == null
                || dept.getOrgCode() == null || dept.getDeptCode() == null) {
            throw new IllegalArgumentException("tenantCode / domainCode / orgCode / deptCode 必填");
        }
        if (dept.getDeptName() == null) throw new IllegalArgumentException("deptName 必填");


        DeptDO exist = deptDbService.selectByDeptCode(dept.getTenantCode(), dept.getDomainCode(), dept.getDeptCode());
        if (exist != null) {
            throw new IllegalStateException("部门编码已存在: " + dept.getDeptCode());
        }

        if (dept.getStatus() == null) dept.setStatus(1);

        dept.setCreatedAt(LocalDateTime.now());
        dept.setUpdatedAt(LocalDateTime.now());
        dept.setCreatedBy(createdBy);
        dept.setUpdatedBy(createdBy);
        if (dept.getExtConfig() == null) dept.setExtConfig("[]");


        deptDbService.insert(dept);
        log.info("创建部门: tenant={}, domain={}, orgCode={}, deptCode={}, id={}",
                dept.getTenantCode(), dept.getDomainCode(), dept.getOrgCode(), dept.getDeptCode(), dept.getId());
        return dept.getId();
    }

    /**
     * 根据租户/域/组织/部门编码查询部门.
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param deptCode   部门编码
     * @return 部门信息，不存在则返回空
     */
    @Override
    public Optional<DeptDO> findByCode(String tenantCode, String domainCode, String deptCode) {
        return Optional.ofNullable(deptDbService.selectByDeptCode(tenantCode, domainCode, deptCode));
    }

    /**
     * 查询组织下的所有部门.
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param orgCode    组织编码
     * @return 部门列表
     */
    @Override
    public List<DeptDO> listByOrg(String tenantCode, String domainCode, String orgCode) {
        if (tenantCode == null || domainCode == null || orgCode == null) { return Collections.emptyList(); }

        return deptDbService.selectByOrgCode(tenantCode, domainCode, orgCode);
    }

    /**
     * 更新部门信息.
     *
     * @param tenantCode  租户编码
     * @param domainCode  域编码
     * @param deptCode    部门编码
     * @param patch       更新内容
     * @param updatedBy   更新人
     * @return true 更新成功，false 部门不存在或更新失败
     */
    @Override
    @Transactional
    public boolean updateDept(String tenantCode, String domainCode, String deptCode, DeptDO patch, String updatedBy) {
        if (tenantCode == null || domainCode == null || deptCode == null || patch == null) { return false; }

        DeptDO exist = deptDbService.selectByDeptCode(tenantCode, domainCode, deptCode);
        if (exist == null) { return false; }

        if (patch.getDeptName() != null) exist.setDeptName(patch.getDeptName());

        if (patch.getDescription() != null) exist.setDescription(patch.getDescription());

        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());

        if (patch.getExtConfig() != null) exist.setExtConfig(patch.getExtConfig());

        exist.setUpdatedAt(LocalDateTime.now());
        exist.setUpdatedBy(updatedBy);
        return deptDbService.updateById(exist) > 0;
    }

    /**
     * 删除部门.
     *
     * @param tenantCode  租户编码
     * @param domainCode  域编码
     * @param deptCode    部门编码
     * @return true 删除成功，false 部门不存在或删除失败
     */
    @Override
    @Transactional
    public boolean deleteDept(String tenantCode, String domainCode, String deptCode) {
        DeptDO exist = deptDbService.selectByDeptCode(tenantCode, domainCode, deptCode);
        if (exist == null) { return false; }

        return deptDbService.deleteById(exist.getId()) > 0;
    }
}
