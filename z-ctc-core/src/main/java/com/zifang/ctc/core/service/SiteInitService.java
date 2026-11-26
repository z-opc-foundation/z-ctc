package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.SiteInitRegistrationDO;
import com.zifang.ctc.core.domain.entity.SiteInitTaskDO;

import java.util.List;

/**
 * 建站初始化注册表 + 任务记录服务.
 */
public interface SiteInitService {

    /**
     * 注册 (幂等): 同 domainCode 覆盖更新.
     *
     * @return 注册记录
     */
    SiteInitRegistrationDO register(String domainCode, String domainName, Integer orderNo, String endpoint,
                                     String protocolVersion, String capabilities);

    /** 全部注册记录 (含停用), 按 order_no、domain_code 排序 */
    List<SiteInitRegistrationDO> listAll();

    /** 启用的注册记录, 按 order_no、domain_code 排序 (编排用) */
    List<SiteInitRegistrationDO> listActive();

    /** 停用某域注册; 域不存在返回 false */
    boolean disable(String domainCode);

    /** 开一条任务 (待执行 → 执行中) */
    SiteInitTaskDO startTask(String tenantCode, String domainCode, String capability,
                              String idempotencyKey, String requestJson, String traceId);

    /** 收尾一条任务 */
    void finishTask(Long taskId, int status, String responseJson, String message, long costMs);

    /** 任务详情 */
    SiteInitTaskDO findTask(Long id);

    /** 按幂等键查任务 (重跑判重用) */
    SiteInitTaskDO findByIdempotencyKey(String idempotencyKey);

    /** 任务分页 (tenantCode / domainCode 可空) */
    List<SiteInitTaskDO> pageTasks(String tenantCode, String domainCode, int pageNum, int pageSize, long[] totalOut);

    /** 某站点最近一轮 (最新 traceId) 的全部任务记录; 从未跑过返回空表 */
    List<SiteInitTaskDO> latestTraceTasks(String tenantCode);
}
