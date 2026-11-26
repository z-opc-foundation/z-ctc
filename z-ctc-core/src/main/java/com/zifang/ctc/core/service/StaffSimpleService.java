package com.zifang.ctc.core.service;

import com.zifang.ctc.core.vo.StaffSimpleVO;

import java.util.List;

/**
 * 员工名录服务（对外只读）。
 *
 * <p><b>2026-10-04 新增。</b>消费方 z-opc 的
 * {@code TeamStaffAdapter#fetchAllStaffFromSso} 一直在调用
 * {@code POST /ctc-osc/staff/getStaffSimpleListWithoutLogin}，
 * 该端点<b>本仓从未定义</b>（全 40 仓搜索只命中"被调用"、无任何"定义"）。
 * 没有它，z-team 侧的员工下拉 / 人员搜索 / 花名册全部为空。
 *
 * @author zifang
 * @since 1.0.0
 */
public interface StaffSimpleService {

    /**
     * 列出 (租户) 下的全部正常员工（不含密码等敏感字段）。
     *
     * <p>与 {@link DeptTreeService#buildDeptTree} 同一套 {@code tenantCode}
     * 寻址口径（query → {@code Tenant-Code} 头 → {@code default}）。
     *
     * <p><b>status 过滤</b>：只返回 {@code status = 1}（正常）的账号，
     * 停用账号不进名录。
     *
     * @param tenantCode 租户编码，可为 null（内部回落 default）
     * @return 员工列表；无数据返回空列表（不返回 null）
     */
    List<StaffSimpleVO> listSimpleStaff(String tenantCode);
}
