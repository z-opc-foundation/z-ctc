package com.zifang.ctc.core.vo;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 员工名录条目（对外只读视图）。
 *
 * <p><b>2026-10-04 新增。</b>消费方 z-opc 的
 * {@code TeamStaffAdapter#parseStaffResponse} 期望的响应形态是
 * {@code {code:0, data:[{id,name,jobNumber,accountNo,telephone,extend:{...}}]}}，
 * 且<b>强制要求 {@code extend} 非空</b>（{@code extend == null || extend.isEmpty()} 的记录
 * 会被直接 continue 掉）。
 *
 * <p><b>因此 {@link #extend} 永远返回非 null 的 Map</b>，即使全部字段为空。
 * 这不是冗余防御 —— 消费方有这条过滤逻辑，返回 null 会让整条记录被丢弃。
 *
 * <p><b>字段映射</b>（本仓 {@code z_ctc_ac_user} → 消费方期望名）：
 * <table border="1">
 *   <tr><th>本仓</th><th>消费方字段</th></tr>
 *   <tr><td>{@code id}</td><td>{@code id}（雪花安全：Long，出网转字符串）</td></tr>
 *   <tr><td>{@code user_no}</td><td>{@code jobNumber}</td></tr>
 *   <tr><td>{@code username}</td><td>{@code accountNo}</td></tr>
 *   <tr><td>{@code phone}</td><td>{@code telephone}</td></tr>
 *   <tr><td>{@code nickname}</td><td>{@code name}</td></tr>
 *   <tr><td>{@code email / nickname / avatar / phone}</td><td>{@code extend.*}</td></tr>
 * </table>
 *
 * <p><b>不输出 {@code password_hash}</b>：该端点语义是
 * {@code getStaffSimpleList<strong>WithoutLogin</strong>}，属对外名录。
 */
public class StaffSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 员工主键（Long，出网由 z-boot-jackson-starter 统一转字符串） */
    private Long id;

    /** 工号（映射自 user_no） */
    private String jobNumber;

    /** 账号（映射自 username） */
    private String accountNo;

    /** 姓名（映射自 nickname，回落 username） */
    private String name;

    /** 电话（映射自 phone） */
    private String telephone;

    /**
     * 扩展字段（花名 / 邮箱 / 头像 / 工号等）。
     * <p><b>永不返回 null</b> —— 消费方会把 extend 为空的记录整条丢弃。
     */
    private Map<String, Object> extend = new LinkedHashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(String jobNumber) {
        this.jobNumber = jobNumber;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Map<String, Object> getExtend() {
        return extend;
    }

    public void setExtend(Map<String, Object> extend) {
        this.extend = extend == null ? new LinkedHashMap<>() : extend;
    }
}
