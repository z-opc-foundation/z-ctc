package com.zifang.ctc.core.init;

/**
 * 租户初始化上下文. 各模块的 {@link TenantInitializer} 在被调用时收到这个上下文.
 *
 * <p>作用:
 * <ul>
 *   <li>让 Initializer 知道是为哪个新租户工作 (tenantCode)</li>
 *   <li>让 Initializer 知道触发人 (createdBy - 来自 X-User-Id, 可能是 null)</li>
 *   <li>让 Initializer 知道租户 id (tenantId - 主键)</li>
 *   <li>timestamp: 租户创建时间, 用于时间戳一致性</li>
 * </ul>
 *
 * <p>线程安全: 字段 final, 不可变; 多个 Initializer 之间共享时无副作用.
 * 用普通 class (非 record) 是因为项目用 Java 8, record 要 Java 16+.
 */
public final class TenantInitContext {

    private final String tenantCode;
    private final Long tenantId;
    private final Long createdBy;
    private final long timestamp;

    public TenantInitContext(String tenantCode, Long tenantId, Long createdBy, long timestamp) {
        this.tenantCode = tenantCode;
        this.tenantId = tenantId;
        this.createdBy = createdBy;
        this.timestamp = timestamp;
    }

    /**
     * 工厂方法: 当前时间戳, 简化调用方.
     */
    public static TenantInitContext of(String tenantCode, Long tenantId, Long createdBy) {
        return new TenantInitContext(tenantCode, tenantId, createdBy, System.currentTimeMillis());
    }

    public String tenantCode() {
        return tenantCode;
    }

    public Long tenantId() {
        return tenantId;
    }

    public Long createdBy() {
        return createdBy;
    }

    public long timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "TenantInitContext{tenantCode='" + tenantCode + "', tenantId=" + tenantId
                + ", createdBy=" + createdBy + ", timestamp=" + timestamp + "}";
    }
}
