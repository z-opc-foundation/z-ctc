package com.zifang.ctc.web.api;

import com.zifang.util.core.meta.BaseStatusCode;
import com.zifang.util.core.meta.Result;
import com.zifang.ctc.core.service.StaffSimpleService;
import com.zifang.ctc.core.vo.StaffSimpleVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 员工名录 Controller（{@code /ctc-osc/staff} 前缀）。
 *
 * <p><b>2026-10-04 新增。</b>补齐消费方 z-opc
 * {@code TeamStaffAdapter#fetchAllStaffFromSso} 一直在调用、但本仓从未定义的
 * {@code POST /ctc-osc/staff/getStaffSimpleListWithoutLogin}。
 * 此前没有它，z-team 侧所有依赖员工名录的功能（人员下拉、搜索、花名册、
 * 部门人员关联）拿到的都是空列表。
 *
 * <p>与 {@link OscDeptController} 同属 {@code /ctc-osc} 命名空间，
 * 寻址口径也一致（query tenantCode → {@code Tenant-Code} 头 → {@code default}）。
 *
 * <p><b>为什么端点名带 {@code WithoutLogin}</b>：沿用消费方的既有命名，
 * 表示该接口<b>不要求登录态</b>（服务端到服务端调用，凭 Tenant-Code 定位租户）。
 * 但返回内容严格剔除 {@code password_hash} —— "不需要登录"不等于"可以外泄凭据"。
 *
 * @author zifang
 * @since 1.0.0
 */
@RestController("ctcOscStaffController")
@RequestMapping("/ctc-osc/staff")
public class OscStaffController {

    private static final Logger log = LogManager.getLogger(OscStaffController.class);

    private final StaffSimpleService staffSimpleService;

    public OscStaffController(StaffSimpleService staffSimpleService) {
        this.staffSimpleService = staffSimpleService;
    }

    /**
     * 获取全部员工名录（不含密码等敏感字段）。
     *
     * <p>响应契约：{@code {code:0, data:[{id,jobNumber,accountNo,name,telephone,extend:{...}}]}}。
     * 消费方 {@code parseStaffResponse} 会<b>丢弃 extend 为空的记录</b>，
     * 故 {@link StaffSimpleVO#getExtend()} 保证非 null。
     *
     * <p>同时提供 GET 便于浏览器直连排查（消费方用 POST 是历史遗留）。
     *
     * @param tenantCode        租户编码；缺省时读 {@code Tenant-Code} 头，再缺省 {@code default}
     * @param tenantCodeHeader  消费方实际发送的请求头
     * @return 员工列表；无数据返回空数组
     */
    @PostMapping("/getStaffSimpleListWithoutLogin")
    public Result<List<StaffSimpleVO>> getStaffSimpleListWithoutLogin(
            @RequestParam(required = false, defaultValue = "") String tenantCode,
            @RequestHeader(value = "Tenant-Code", required = false) String tenantCodeHeader) {
        try {
            return Result.success(staffSimpleService.listSimpleStaff(resolveTenant(tenantCode, tenantCodeHeader)));
        } catch (Exception e) {
            log.warn("[OscStaffController] 查询员工名录失败: {}", e.toString());
            return Result.error(BaseStatusCode.FAIL, "查询员工名录失败: " + e.getMessage());
        }
    }

    /** GET 别名，语义与 POST 版完全相同（供浏览器直连排查用）。 */
    @GetMapping("/getStaffSimpleListWithoutLogin")
    public Result<List<StaffSimpleVO>> getStaffSimpleListWithoutLoginGet(
            @RequestParam(required = false, defaultValue = "") String tenantCode,
            @RequestHeader(value = "Tenant-Code", required = false) String tenantCodeHeader) {
        return getStaffSimpleListWithoutLogin(tenantCode, tenantCodeHeader);
    }

    /** 租户编码优先级：query 参数 → Tenant-Code 头 → {@code default}。 */
    private static String resolveTenant(String fromQuery, String fromHeader) {
        if (fromQuery != null && !fromQuery.trim().isEmpty()) {
            return fromQuery.trim();
        }
        if (fromHeader != null && !fromHeader.trim().isEmpty()) {
            return fromHeader.trim();
        }
        return "default";
    }
}
