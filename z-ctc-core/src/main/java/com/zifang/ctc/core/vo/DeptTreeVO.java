package com.zifang.ctc.core.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 人事架构部门树节点 (对外视图).
 *
 * <p><b>2026-10-04 新增。</b>z-ctc 原先只有 {@link org}/{@link DeptVO}/{@link org}
 * 的<b>平铺 CRUD</b>，没有树形接口；而 z-opc 侧 {@code TeamStaffAdapter.fetchOrgTree}
 * 一直在调用 {@code POST /ctc-osc/dept/getStaffDeptTree}，该端点<b>本仓从未定义</b>
 * （全 40 仓搜索只命中"被调用"、无任何"定义"）。
 *
 * <p>本 VO 即为该端点的响应契约，字段名刻意与消费方保持一致：
 * <ul>
 *   <li>{@code id} / {@code name} — 部门标识与名称（消费方 {@code deptIdOf}/{@code deptNameOf}）</li>
 *   <li>{@code parentId} / {@code parentName} — 父部门（消费方按 {@code parentId} 组树）</li>
 *   <li>{@code childrenList} — 子部门（消费方 {@code childrenOf} 兼容
 *       {@code childrenList} → {@code children}）</li>
 *   <li>{@code staffInfos} — <b>直属人员</b>（消费方 {@code staffIdsOfNode} 兼容
 *       {@code staffInfos} → {@code staffList} → {@code staffIds}）</li>
 * </ul>
 *
 * <p><b>为什么用 {@code staffInfos} 而非 {@code staffIds}</b>：本仓
 * {@code MembershipDO.deptCode} → {@code UserDO.id} 的关联天然产出"人 + 姓名"，
 * 而消费方需要姓名做展示。这里仍由消费方的三 key 兼容兜底，
 * 避免任何一侧再出现"只认其中一个 key"的第 5 种读法。
 *
 * <p><b>雪花 ID 安全</b>：{@code id} 为 Long，出网由 z-boot-jackson-starter
 * 统一转字符串（见 z-team 侧分层契约），此处不做手工转换。
 */
public class DeptTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 部门主键（Long，出网转字符串） */
    private Long id;

    /** 部门编码（业务可读标识，租户域内唯一） */
    private String deptCode;

    /** 部门名称 */
    private String name;

    /** 父部门主键；根节点为 null */
    private Long parentId;

    /** 父部门编码；根节点为 null */
    private String parentCode;

    /** 所属组织编码 */
    private String orgCode;

    /** 状态：0=停用 1=正常 */
    private Integer status;

    /** 子部门列表；叶子节点为空数组（不返回 null，省得消费方到处判空） */
    private List<DeptTreeVO> childrenList = new ArrayList<>();

    /** 本部门直属人员 */
    private List<StaffVO> staffInfos = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<DeptTreeVO> getChildrenList() {
        return childrenList;
    }

    public void setChildrenList(List<DeptTreeVO> childrenList) {
        this.childrenList = childrenList == null ? new ArrayList<>() : childrenList;
    }

    public List<StaffVO> getStaffInfos() {
        return staffInfos;
    }

    public void setStaffInfos(List<StaffVO> staffInfos) {
        this.staffInfos = staffInfos == null ? new ArrayList<>() : staffInfos;
    }

    /**
     * 部门直属人员视图。
     *
     * <p>{@code id} 即消费方用于匹配 {@code /current-dept} 返回 staffId 的键
     * （两边同为 {@code z_ctc_ac_user.id}）。
     */
    public static class StaffVO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;
        private String userNo;
        private String username;
        private String nickname;
        private String email;

        public StaffVO() {
        }

        public StaffVO(Long id, String userNo, String username, String nickname, String email) {
            this.id = id;
            this.userNo = userNo;
            this.username = username;
            this.nickname = nickname;
            this.email = email;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUserNo() {
            return userNo;
        }

        public void setUserNo(String userNo) {
            this.userNo = userNo;
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
