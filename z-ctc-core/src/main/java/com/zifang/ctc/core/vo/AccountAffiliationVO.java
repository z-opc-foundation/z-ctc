package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.DeptDO;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.entity.OrgDO;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 账号的组织归属视图 — 账户详情反查「挂在哪个租户/组织/部门上」.
 * <p>
 * membership 模型只有 dept 维度: dept_code → dept (deptName/orgCode/domainCode) → org (orgName).
 * 无 dept_code 的成员关系 (如 owner 创建租户) 只落到租户一级.
 */
public class AccountAffiliationVO implements Serializable {

    private static final long serialVersionUID = 1L;
    private String tenantCode;
    private String roleCode;
    private Integer memberStatus;
    private LocalDateTime joinedAt;
    private String domainCode;
    private String orgCode;
    private String orgName;
    private String deptCode;
    private String deptName;

    public static AccountAffiliationVO from(MembershipDO m, DeptDO d, OrgDO o) {
        if (m == null) { return null; }

        AccountAffiliationVO v = new AccountAffiliationVO();
        v.tenantCode = m.getTenantCode();
        v.roleCode = m.getRoleCode();
        v.memberStatus = m.getMemberStatus();
        v.joinedAt = m.getJoinedAt();
        v.deptCode = m.getDeptCode();
        if (d != null) {
            v.deptName = d.getDeptName();
            v.domainCode = d.getDomainCode();
            v.orgCode = d.getOrgCode();
        }
        if (o != null) {
            v.orgName = o.getOrgName();
        }
        return v;
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

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
}
