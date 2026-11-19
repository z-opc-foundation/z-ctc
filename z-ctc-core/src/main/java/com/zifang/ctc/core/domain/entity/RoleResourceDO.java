package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

@BizModel(name = "z_ctc_spc_role_resource", module = "z-ctc", description = "z_ctc_spc_role_resource")
@TableName("z_ctc_spc_role_resource")
public class RoleResourceDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("role_id")
    @BizField(name = "角色 ID", type = BizField.FieldType.INTEGER)
    private Long roleId;

    @TableField("resource_id")
    @BizField(name = "资源 ID", type = BizField.FieldType.INTEGER)
    private Long resourceId;

    @TableField("data_filter")
    @BizField(name = "数据权限过滤表达式 (SpEL, 仅 resource_type=2 时生效, 如 #user.tenantCode == #target.tenantCode)", type = BizField.FieldType.STRING)
    private String dataFilter;

    @TableField("granted_at")
    @BizField(name = "授权时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime grantedAt;

    @TableField("granted_by")
    @BizField(name = "授权人", type = BizField.FieldType.STRING)
    private String grantedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getDataFilter() {
        return dataFilter;
    }

    public void setDataFilter(String dataFilter) {
        this.dataFilter = dataFilter;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }
}



