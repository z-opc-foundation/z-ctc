package com.zifang.ctc.core.ak;

/**
 * AK 域的调用人（租户 + 操作人）。
 *
 * <p>与 z-kb 的 KbCaller 同口径，但放在 z-ctc 自己的命名空间下：AK 台账的真源在
 * 认证中心，业务服务（z-kb 等）通过 HTTP 调用过来时由本层从可信上下文解析。</p>
 *
 * @author zifang
 */
public class AkCaller {

    private String tenantCode;
    private String actorId;
    private String actorName;

    public AkCaller() {
    }

    public AkCaller(String tenantCode, String actorId, String actorName) {
        this.tenantCode = tenantCode;
        this.actorId = actorId;
        this.actorName = actorName;
    }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    /** 是否同一个操作人（AK 归属校验）。 */
    public boolean isSameActor(String other) {
        return actorId != null && actorId.equals(other);
    }
}
