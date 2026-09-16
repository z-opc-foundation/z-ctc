package com.zifang.ctc.core.init;

import java.util.ArrayList;
import java.util.List;

/**
 * 租户初始化执行报告. 用于日志/管理端展示/测试断言.
 *
 * <p>数据结构: 记录每个模块的耗时与成败.
 */
public class TenantInitReport {

    private final String tenantCode;
    private final List<Entry> entries = new ArrayList<>();
    private long totalCostMs = 0L;
    private long startedAt = System.currentTimeMillis();

    public TenantInitReport(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public void addSuccess(String module, long costMs) {
        entries.add(new Entry(module, true, null, costMs));
        totalCostMs += costMs;
    }

    public void addFailure(String module, Throwable t) {
        long now = System.currentTimeMillis();
        entries.add(new Entry(module, false, t == null ? "未知错误" : t.getClass().getSimpleName() + ": " + t.getMessage(), 0L));
        // 失败时 cost 不累加 (无法准确测量失败点耗时)
    }

    public int getSuccessCount() {
        return (int) entries.stream().filter(e -> e.success).count();
    }

    public int getFailureCount() {
        return (int) entries.stream().filter(e -> !e.success).count();
    }

    public long getTotalCostMs() {
        // 总耗时 = 报告生成时刻 - 报告创建时刻 (更准确, 因为失败不累加)
        return System.currentTimeMillis() - startedAt;
    }

    public List<Entry> getEntries() {
        return new ArrayList<>(entries);
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public boolean isAllSuccess() {
        return entries.stream().allMatch(e -> e.success);
    }

    public static class Entry {
        public final String module;
        public final boolean success;
        public final String errorMessage;
        public final long costMs;

        public Entry(String module, boolean success, String errorMessage, long costMs) {
            this.module = module;
            this.success = success;
            this.errorMessage = errorMessage;
            this.costMs = costMs;
        }
    }
}
