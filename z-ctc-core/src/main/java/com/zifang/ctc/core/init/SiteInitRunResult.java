package com.zifang.ctc.core.init;

import com.zifang.ctc.core.domain.entity.SiteInitRegistrationDO;

import java.util.ArrayList;
import java.util.List;

/**
 * 建站初始化执行汇总 (ctc 侧): 一次建站/重跑覆盖的所有域的结果.
 */
public class SiteInitRunResult {

    private final String tenantCode;
    private final List<DomainResult> domains = new ArrayList<>();
    private long totalCostMs = 0L;
    private final long startedAt = System.currentTimeMillis();

    public SiteInitRunResult(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public void addDomain(SiteInitRegistrationDO registration, boolean success, String message, long costMs) {
        domains.add(new DomainResult(registration.getDomainCode(), registration.getDomainName(),
                registration.getEndpoint(), success, message, costMs));
        totalCostMs += costMs;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public List<DomainResult> getDomains() {
        return domains;
    }

    public int getSuccessCount() {
        return (int) domains.stream().filter(d -> d.success).count();
    }

    public int getFailureCount() {
        return (int) domains.stream().filter(d -> !d.success).count();
    }

    public long getTotalCostMs() {
        return totalCostMs;
    }

    public long getElapsedMs() {
        return System.currentTimeMillis() - startedAt;
    }

    public boolean isAllSuccess() {
        return domains.stream().allMatch(d -> d.success);
    }

    /** 单域执行结果 */
    public static class DomainResult {
        public final String domainCode;
        public final String domainName;
        public final String endpoint;
        public final boolean success;
        public final String message;
        public final long costMs;

        public DomainResult(String domainCode, String domainName, String endpoint,
                            boolean success, String message, long costMs) {
            this.domainCode = domainCode;
            this.domainName = domainName;
            this.endpoint = endpoint;
            this.success = success;
            this.message = message;
            this.costMs = costMs;
        }
    }
}
