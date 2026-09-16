package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.GroupDO;
import com.zifang.ctc.core.domain.service.GroupDbService;
import com.zifang.ctc.core.service.GroupService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * GroupService 默认实现：业务语义 + 事务，持久化委托给 {@link GroupDbService}。
 *
 * @author zifang
 * @since 1.0.0
 * @see GroupService
 */
@Service
public class AcGroupServiceImpl implements GroupService {

    private static final Logger log = LogManager.getLogger(AcGroupServiceImpl.class);

    private final GroupDbService groupDbService;

    /**
     * 构造函数。
     *
     * @param groupDbService 组别数据服务
     */
    public AcGroupServiceImpl(GroupDbService groupDbService) {
        this.groupDbService = groupDbService;
    }

    /**
     * 创建组别。
     *
     * @param group      组别实体对象
     * @param createdBy  创建人
     * @return 组别主键
     * @throws IllegalArgumentException 当 group 为空或必填字段为空时
     * @throws IllegalStateException    当组别编码已存在时
     */
    @Override
    @Transactional
    public Long createGroup(GroupDO group, String createdBy) {
        if (group == null) { throw new IllegalArgumentException("group 不能为空"); }

        if (group.getTenantCode() == null || group.getDomainCode() == null
                || group.getOrgCode() == null || group.getDeptCode() == null
                || group.getGroupCode() == null) {
            throw new IllegalArgumentException("tenantCode / domainCode / orgCode / deptCode / groupCode 必填");
        }
        if (group.getGroupName() == null) throw new IllegalArgumentException("groupName 必填");


        GroupDO exist = groupDbService.selectByGroupCode(group.getTenantCode(), group.getDomainCode(), group.getGroupCode());
        if (exist != null) {
            throw new IllegalStateException("组别编码已存在: " + group.getGroupCode());
        }

        if (group.getStatus() == null) group.setStatus(1);

        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        group.setCreatedBy(createdBy);
        group.setUpdatedBy(createdBy);
        if (group.getExtConfig() == null) group.setExtConfig("[]");


        groupDbService.insert(group);
        log.info("创建组别: tenant={}, domain={}, deptCode={}, groupCode={}, id={}",
                group.getTenantCode(), group.getDomainCode(), group.getDeptCode(), group.getGroupCode(), group.getId());
        return group.getId();
    }

    /**
     * 根据编码查找组别。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param groupCode  组别编码
     * @return 组别实体 Optional
     */
    @Override
    public Optional<GroupDO> findByCode(String tenantCode, String domainCode, String groupCode) {
        return Optional.ofNullable(groupDbService.selectByGroupCode(tenantCode, domainCode, groupCode));
    }

    /**
     * 根据部门编码查询组别列表。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param deptCode   部门编码
     * @return 组别实体列表
     */
    @Override
    public List<GroupDO> listByDept(String tenantCode, String domainCode, String deptCode) {
        if (tenantCode == null || domainCode == null || deptCode == null) { return Collections.emptyList(); }

        return groupDbService.selectByDeptCode(tenantCode, domainCode, deptCode);
    }

    /**
     * 更新组别。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param groupCode  组别编码
     * @param patch      更新内容
     * @param updatedBy  更新人
     * @return 是否更新成功
     */
    @Override
    @Transactional
    public boolean updateGroup(String tenantCode, String domainCode, String groupCode, GroupDO patch, String updatedBy) {
        if (tenantCode == null || domainCode == null || groupCode == null || patch == null) { return false; }

        GroupDO exist = groupDbService.selectByGroupCode(tenantCode, domainCode, groupCode);
        if (exist == null) { return false; }

        if (patch.getGroupName() != null) exist.setGroupName(patch.getGroupName());

        if (patch.getDescription() != null) exist.setDescription(patch.getDescription());

        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());

        if (patch.getExtConfig() != null) exist.setExtConfig(patch.getExtConfig());

        exist.setUpdatedAt(LocalDateTime.now());
        exist.setUpdatedBy(updatedBy);
        return groupDbService.updateById(exist) > 0;
    }

    /**
     * 删除组别。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param groupCode  组别编码
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteGroup(String tenantCode, String domainCode, String groupCode) {
        GroupDO exist = groupDbService.selectByGroupCode(tenantCode, domainCode, groupCode);
        if (exist == null) { return false; }

        return groupDbService.deleteById(exist.getId()) > 0;
    }
}
