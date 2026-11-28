package com.zifang.ctc.core.ak.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.Instant;

/** AK 台账（z_ctc_ak）：密钥只存哈希，明文仅创建响应返回一次。 */
@BizModel(name = "AK 台账（z_ctc_ak）：密钥只存哈希，明文仅创建响应返回一次", module = "z-ctc", description = "AK 台账（z_ctc_ak）：密钥只存哈希，明文仅创建响应返回一次。")
@TableName("z_ctc_ak")
public class AkDO {

    @TableId(type = IdType.INPUT)
    @BizField(name = "id", type = BizField.FieldType.STRING)
    private String id;
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "AK 编码", type = BizField.FieldType.STRING)
    private String akCode;
    @BizField(name = "AK 名称", type = BizField.FieldType.STRING)
    private String akName;
    @BizField(name = "归属类型", type = BizField.FieldType.STRING)
    private String ownerType;
    @BizField(name = "归属人", type = BizField.FieldType.STRING)
    private String ownerId;
    @BizField(name = "归属人姓名", type = BizField.FieldType.STRING)
    private String ownerName;
    @BizField(name = "密钥哈希", type = BizField.FieldType.STRING)
    private String secretHash;
    @BizField(name = "状态 1-启用", type = BizField.FieldType.INTEGER)
    private Integer status;
    private Integer isDeleted;
    private String createBy;
    private String updateBy;
    private Instant createTime;
    private Instant updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getAkCode() {
        return akCode;
    }

    public void setAkCode(String akCode) {
        this.akCode = akCode;
    }

    public String getAkName() {
        return akName;
    }

    public void setAkName(String akName) {
        this.akName = akName;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public void setSecretHash(String secretHash) {
        this.secretHash = secretHash;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }
}