package com.zifang.ctc.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.ctc.core.domain.entity.SiteInitRegistrationDO;
import com.zifang.ctc.core.domain.entity.SiteInitTaskDO;
import com.zifang.ctc.core.domain.mapper.SiteInitRegistrationMapper;
import com.zifang.ctc.core.domain.mapper.SiteInitTaskMapper;
import com.zifang.ctc.core.service.SiteInitService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 建站初始化注册表 + 任务记录服务实现.
 */
@Service
public class SiteInitServiceImpl implements SiteInitService {

    private static final Logger log = LogManager.getLogger(SiteInitServiceImpl.class);

    private final SiteInitRegistrationMapper registrationMapper;
    private final SiteInitTaskMapper taskMapper;

    public SiteInitServiceImpl(SiteInitRegistrationMapper registrationMapper, SiteInitTaskMapper taskMapper) {
        this.registrationMapper = registrationMapper;
        this.taskMapper = taskMapper;
    }

    @Override
    @Transactional
    public SiteInitRegistrationDO register(String domainCode, String domainName, Integer orderNo, String endpoint,
                                          String protocolVersion, String capabilities) {
        SiteInitRegistrationDO existing = registrationMapper.selectByDomainCode(domainCode);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            existing = new SiteInitRegistrationDO();
            existing.setDomainCode(domainCode);
            existing.setCreatedAt(now);
        }
        existing.setDomainName(domainName);
        existing.setOrderNo(orderNo == null ? 100 : orderNo);
        existing.setEndpoint(endpoint);
        existing.setProtocolVersion(protocolVersion);
        existing.setCapabilities(capabilities);
        existing.setStatus(1);
        existing.setRegisteredAt(now);
        existing.setUpdatedAt(now);
        if (existing.getId() == null) {
            registrationMapper.insert(existing);
        } else {
            registrationMapper.updateById(existing);
        }
        log.info("[SiteInit] 域注册成功: domainCode={} order={} endpoint={} capabilities={}",
                domainCode, existing.getOrderNo(), endpoint, capabilities);
        return existing;
    }

    @Override
    public List<SiteInitRegistrationDO> listAll() {
        return registrationMapper.selectList(new QueryWrapper<SiteInitRegistrationDO>()
                .orderByAsc("order_no", "domain_code"));
    }

    @Override
    public List<SiteInitRegistrationDO> listActive() {
        return registrationMapper.selectList(new QueryWrapper<SiteInitRegistrationDO>()
                .eq("status", 1)
                .orderByAsc("order_no", "domain_code"));
    }

    @Override
    @Transactional
    public boolean disable(String domainCode) {
        SiteInitRegistrationDO existing = registrationMapper.selectByDomainCode(domainCode);
        if (existing == null) {
            return false;
        }
        existing.setStatus(0);
        existing.setUpdatedAt(LocalDateTime.now());
        registrationMapper.updateById(existing);
        log.info("[SiteInit] 域注册已停用: domainCode={}", domainCode);
        return true;
    }

    @Override
    @Transactional
    public SiteInitTaskDO startTask(String tenantCode, String domainCode, String capability,
                                     String idempotencyKey, String requestJson, String traceId) {
        SiteInitTaskDO task = new SiteInitTaskDO();
        task.setTenantCode(tenantCode);
        task.setDomainCode(domainCode);
        task.setCapability(capability);
        task.setStatus(SiteInitTaskDO.STATUS_RUNNING);
        task.setIdempotencyKey(idempotencyKey);
        task.setRequestJson(requestJson);
        task.setTraceId(traceId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    @Override
    @Transactional
    public void finishTask(Long taskId, int status, String responseJson, String message, long costMs) {
        SiteInitTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(status);
        task.setResponseJson(responseJson);
        task.setMessage(message);
        task.setCostMs(costMs);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    @Override
    public SiteInitTaskDO findTask(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public SiteInitTaskDO findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return null;
        }
        return taskMapper.selectOne(new QueryWrapper<SiteInitTaskDO>()
                .eq("idempotency_key", idempotencyKey)
                .last("LIMIT 1"));
    }

    @Override
    public List<SiteInitTaskDO> pageTasks(String tenantCode, String domainCode, int pageNum, int pageSize, long[] totalOut) {
        QueryWrapper<SiteInitTaskDO> qw = new QueryWrapper<>();
        if (tenantCode != null && !tenantCode.isEmpty()) {
            qw.eq("tenant_code", tenantCode);
        }
        if (domainCode != null && !domainCode.isEmpty()) {
            qw.eq("domain_code", domainCode);
        }
        qw.orderByDesc("id");
        IPage<SiteInitTaskDO> page = taskMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        if (totalOut != null && totalOut.length > 0) {
            totalOut[0] = page.getTotal();
        }
        return page.getRecords();
    }

    @Override
    public List<SiteInitTaskDO> latestTraceTasks(String tenantCode) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return List.of();
        }
        SiteInitTaskDO latest = taskMapper.selectOne(new QueryWrapper<SiteInitTaskDO>()
                .eq("tenant_code", tenantCode)
                .orderByDesc("id")
                .last("LIMIT 1"));
        if (latest == null) {
            return List.of();
        }
        return taskMapper.selectList(new QueryWrapper<SiteInitTaskDO>()
                .eq("tenant_code", tenantCode)
                .eq("trace_id", latest.getTraceId())
                .orderByAsc("id"));
    }
}
