package com.zifang.ctc.web.api.response;

import com.zifang.ctc.core.domain.entity.MembershipDO;

import java.util.List;

public class MyTenantsResult {

    private Long userId;
    private String userNo;
    private String email;
    private String username;
    private String nickname;
    private List<MembershipDO> memberships;
    private int tenantCount;
    private boolean isOrphan;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public List<MembershipDO> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<MembershipDO> memberships) {
        this.memberships = memberships;
    }

    public int getTenantCount() {
        return tenantCount;
    }

    public void setTenantCount(int tenantCount) {
        this.tenantCount = tenantCount;
    }

    public boolean isOrphan() {
        return isOrphan;
    }

    public void setOrphan(boolean orphan) {
        isOrphan = orphan;
    }
}
