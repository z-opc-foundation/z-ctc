package com.zifang.ctc.core.service.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

/**
 * 机器 / 服务账号会话（z_ctc_service_session）：令牌只存哈希，明文只在签发那一次响应返回。
 *
 * <p>给设备控制台一类的机器访问用（BIOS 控制台等）：签发、验签、吊销都在认证中心，
 * 业务域不再自建令牌表与自造校验拦截器。</p>
 */
@BizModel(name = "机器 / 服务账号会话（z_ctc_service_session）", module = "z-ctc",
        description = "机器 / 服务账号会话：令牌只存哈希，明文仅签发那一次响应返回。")
@TableName("z_ctc_service_session")
public class ServiceSessionDO {

    @TableId(type = IdType.INPUT)
    @BizField(name = "id", type = BizField.FieldType.STRING)
    private String id;
    @BizField(name = "租户", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "主体类型", type = BizField.FieldType.STRING)
    private String subjectType;
    @BizField(name = "主体标识", type = BizField.FieldType.STRING)
    private String subjectCode;
    @BizField(name = "令牌哈希", type = BizField.FieldType.STRING)
    private String tokenHash;
    @BizField(name = "签发时间", type = BizField.FieldType.INTEGER)
    private long issuedAt;
    @BizField(name = "过期时间", type = BizField.FieldType.INTEGER)
    private long expiresAt;
    @BizField(name = "最近活跃", type = BizField.FieldType.INTEGER)
    private long lastSeenAt;
    @BizField(name = "是否吊销", type = BizField.FieldType.INTEGER)
    private int revoked;
    @BizField(name = "创建时间", type = BizField.FieldType.INTEGER)
    private long createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public long getIssuedAt() { return issuedAt; }
    public void setIssuedAt(long issuedAt) { this.issuedAt = issuedAt; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public long getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(long lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public int getRevoked() { return revoked; }
    public void setRevoked(int revoked) { this.revoked = revoked; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
