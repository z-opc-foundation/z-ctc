package com.zifang.ctc.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.ctc.core.domain.entity.TenantDO;
import com.zifang.ctc.core.domain.service.TenantDbService;
import com.zifang.ctc.core.service.TenantService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 租户服务实现类.
 * <p>
 * 业务语义（租户编码唯一性校验 + 状态控制） + 事务,
 * 持久化委托给 {@link TenantDbService}.
 *
 * @author zifang
 * @since 1.0.0
 * @see TenantService
 * @see TenantDbService
 */
@Service
public class AcTenantServiceImpl implements TenantService {

    private static final Logger log = LogManager.getLogger(AcTenantServiceImpl.class);

    private final TenantDbService tenantDbService;

    public AcTenantServiceImpl(TenantDbService tenantDbService) {
        this.tenantDbService = tenantDbService;
    }

    /**
     * 创建租户.
     *
     * @param tenant    租户信息，必须包含 tenantCode 和 tenantName
     * @param createdBy 创建人
     * @return 租户ID
     * @throws IllegalArgumentException 当 tenant 为空或必填字段缺失时
     */
    @Override
    @Transactional
    public Long createTenant(TenantDO tenant, String createdBy) {
        if (tenant == null || tenant.getTenantCode() == null || tenant.getTenantName() == null) {
            throw new IllegalArgumentException("tenantCode/tenantName required");
        }
        if (tenant.getStatus() == null) tenant.setStatus(1);

        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());
        tenant.setCreatedBy(createdBy);
        tenantDbService.insert(tenant);
        return tenant.getId();
    }

    /**
     * 根据租户编码查询租户.
     *
     * @param tenantCode 租户编码
     * @return 租户信息，不存在则返回空
     */
    @Override
    public Optional<TenantDO> findByTenantCode(String tenantCode) {
        return Optional.ofNullable(tenantDbService.selectByTenantCode(tenantCode));
    }

    /**
     * 更新租户状态.
     *
     * @param tenantCode 租户编码
     * @param newStatus  新状态
     * @return true 更新成功，false 租户不存在或更新失败
     */
    @Override
    @Transactional
    public boolean updateStatus(String tenantCode, int newStatus) {
        TenantDO t = tenantDbService.selectByTenantCode(tenantCode);
        if (t == null) { return false; }

        t.setStatus(newStatus);
        t.setUpdatedAt(LocalDateTime.now());
        return tenantDbService.updateById(t) > 0;
    }

    /**
     * 查询所有租户（按创建时间倒序）.
     *
     * @return 租户列表
     */
    @Override
    public List<TenantDO> listAll() {
        return tenantDbService.selectByQuery(new QueryWrapper<TenantDO>().orderByDesc("created_at"));
    }

    /**
     * 分页查询租户（支持关键词搜索）.
     *
     * @param keyword  关键词（tenantCode 或 tenantName 模糊匹配）
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页大小
     * @param total    总记录数（用于分页组件，非必须）
     * @return 租户列表
     */
    @Override
    public List<TenantDO> pageList(String keyword, int pageNum, int pageSize, long total) {
        Page<TenantDO> page = new Page<>(pageNum, pageSize);
        QueryWrapper<TenantDO> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("tenant_code", keyword).or().like("tenant_name", keyword));
        }
        qw.orderByDesc("created_at");
        IPage<TenantDO> result = tenantDbService.selectPage(page, qw);
        if (total > 0) { result.setTotal(total); }

        return result.getRecords();
    }

    /**
     * 更新租户信息.
     *
     * @param tenantCode 租户编码
     * @param patch      更新内容
     * @return true 更新成功，false 租户不存在或更新失败
     */
    @Override
    @Transactional
    public boolean updateTenant(String tenantCode, TenantDO patch) {
        if (tenantCode == null || patch == null) { return false; }

        TenantDO exist = tenantDbService.selectByTenantCode(tenantCode);
        if (exist == null) { return false; }

        if (patch.getTenantName() != null) exist.setTenantName(patch.getTenantName());

        if (patch.getContactName() != null) exist.setContactName(patch.getContactName());

        if (patch.getContactPhone() != null) exist.setContactPhone(patch.getContactPhone());

        if (patch.getContactEmail() != null) exist.setContactEmail(patch.getContactEmail());

        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());

        if (patch.getExpireAt() != null) exist.setExpireAt(patch.getExpireAt());

        exist.setUpdatedAt(LocalDateTime.now());
        return tenantDbService.updateById(exist) > 0;
    }

    /**
     * 删除租户.
     *
     * @param tenantCode 租户编码
     * @return true 删除成功，false 租户不存在或删除失败
     */
    @Override
    @Transactional
    public boolean deleteTenant(String tenantCode) {
        TenantDO t = tenantDbService.selectByTenantCode(tenantCode);
        if (t == null) { return false; }

        return tenantDbService.deleteById(t.getId()) > 0;
    }
}
