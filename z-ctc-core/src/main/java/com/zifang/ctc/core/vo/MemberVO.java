package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.entity.UserDO;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织树节点成员视图 (org 节点 = 其下全部 dept 成员并集, dept 节点 = 本部门成员).
 */
public class MemberVO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long userId;
    private String username;
    private String nickname;
    private String tenantCode;
    private String deptCode;
    private String roleCode;
    private Integer memberStatus;
    private LocalDateTime joinedAt;

    public static MemberVO from(MembershipDO m, UserDO u) {
        if (m == null) { return null; }

        MemberVO v = new MemberVO();
        v.userId = m.getUserId();
        v.username = u != null ? u.getUsername() : null;
        v.nickname = u != null ? u.getNickname() : null;
        v.tenantCode = m.getTenantCode();
        v.deptCode = m.getDeptCode();
        v.roleCode = m.getRoleCode();
        v.memberStatus = m.getMemberStatus();
        v.joinedAt = m.getJoinedAt();
        return v;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
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
}
