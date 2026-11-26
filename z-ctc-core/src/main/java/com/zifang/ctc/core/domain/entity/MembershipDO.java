package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * z-ctc 租户成员关系 (FEATURE049).
 * <p>
 * User x Tenant 多对多关联表. 一个 User 可属于多个 Tenant,
 * 在不同 Tenant 中可有不同 role_code.
 * <p>
 * member_status:
 * 0=待接受 1=正常 2=已禁用 3=已退出
 */
@BizModel(name = "z-ctc 租户成员关系", module = "z-ctc", description = "z-ctc 租户成员关系 (FEATURE049). User x Tenant 多对多关联表. 一个 User 可属于多个 Tenant, 在不同 Tenant 中可有不同 role_code. member_status: 0=待接受 1=正常 2=已禁用 3=已退出")
@TableName("z_ctc_ac_membership")
public class MembershipDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("user_id")
    @BizField(name = "用户 ID (关联 z_ctc_ac_user.id)", type = BizField.FieldType.INTEGER)
    private Long userId;

    @TableField("tenant_code")
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("role_code")
    @BizField(name = "租户内角色编码 (冗余, 加速鉴权)", type = BizField.FieldType.STRING)
    private String roleCode;

    @TableField("dept_code")
    @BizField(name = "所属部门 (组织树)", type = BizField.FieldType.STRING)
    private String deptCode;

    /**
     * 0=待接受 1=正常 2=已禁用 3=已退出
     */
    @TableField("member_status")
    @BizField(name = "成员状态: 0=待接受 1=正常 2=已禁用 3=已退出", type = BizField.FieldType.INTEGER)
    private Integer memberStatus;

    @TableField("joined_at")
    @BizField(name = "加入时间 (status→1 时写入)", type = BizField.FieldType.DATETIME)
    private LocalDateTime joinedAt;

    @TableField("left_at")
    @BizField(name = "退出时间 (status→3 时写入)", type = BizField.FieldType.DATETIME)
    private LocalDateTime leftAt;

    @TableField("created_at")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public Integer getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(Integer memberStatus) {
        this.memberStatus = memberStatus;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}



