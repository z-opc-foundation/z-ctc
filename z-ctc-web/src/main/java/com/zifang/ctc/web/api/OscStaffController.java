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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * <p>但返回内容严格剔除 {@code password_hash} —— "不需要登录"不等于"可以外泄凭据"。
 *
 * <p><b>2026-10-21 脱敏收窄（TASK-20261021-034）。</b>本端点挂在公网域
 * {@code ctc.zopc.top} 上，且路径不在 SSO 的 {@code sso.intercept-paths}（{@code /api/**}）
 * 范围内 ⇒ 曾可从公网匿名拖走整份员工名册（姓名 / 工号 / 账号 / 手机号 / 邮箱）。
 * 现**在出网边界收窄返回字段**：只输出 {@code id} / {@code accountNo} / {@code name}
 * 与非 PII 的 {@code extend} 子集，<b>剔除 {@code telephone} / {@code jobNumber} /
 * {@code extend.email} / {@code extend.userNo}</b>。
 * <p>内部服务层（{@link com.zifang.ctc.core.vo.StaffSimpleVO}）仍保留完整映射，
 * 收窄只发生在**出网这一层** ⇒ 仓内其它读法不受影响。
 * <p>⚠ 本端点<b>仍不要求登录态</b>（是否限制调用来源属人工保留项，未在本次改动内处理）
 * ⇒ 收窄后它是「匿名可读的最小名录」，不是「已加固的内部接口」。
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
     * 获取全部员工名录（**脱敏后**的最小字段集）。
     *
     * <p>响应契约（2026-10-21 收窄后）：
     * {@code {code:0, data:[{id,accountNo,name,extend:{nickname,flowerName,avatar?}}]}}。
     * <b>不再输出</b> {@code telephone} / {@code jobNumber} / {@code extend.email} /
     * {@code extend.userNo}（工号）—— 这四个字段曾可被公网匿名拖走。
     * <p>消费方 {@code parseStaffResponse} 会<b>丢弃 extend 为空的记录</b>，
     * 故 {@link PublicStaffVO#getExtend()} 保证非 null。
     *
     * <p>同时提供 GET 便于浏览器直连排查（消费方用 POST 是历史遗留）。
     *
     * @param tenantCode        租户编码；缺省时读 {@code Tenant-Code} 头，再缺省 {@code default}
     * @param tenantCodeHeader  消费方实际发送的请求头
     * @return 员工列表；无数据返回空数组
     */
    @PostMapping("/getStaffSimpleListWithoutLogin")
    public Result<List<PublicStaffVO>> getStaffSimpleListWithoutLogin(
            @RequestParam(required = false, defaultValue = "") String tenantCode,
            @RequestHeader(value = "Tenant-Code", required = false) String tenantCodeHeader) {
        try {
            List<StaffSimpleVO> rows =
                    staffSimpleService.listSimpleStaff(resolveTenant(tenantCode, tenantCodeHeader));
            return Result.success(narrow(rows));
        } catch (Exception e) {
            log.warn("[OscStaffController] 查询员工名录失败: {}", e.toString());
            return Result.error(BaseStatusCode.FAIL, "查询员工名录失败: " + e.getMessage());
        }
    }

    /** GET 别名，语义与 POST 版完全相同（供浏览器直连排查用）。 */
    @GetMapping("/getStaffSimpleListWithoutLogin")
    public Result<List<PublicStaffVO>> getStaffSimpleListWithoutLoginGet(
            @RequestParam(required = false, defaultValue = "") String tenantCode,
            @RequestHeader(value = "Tenant-Code", required = false) String tenantCodeHeader) {
        return getStaffSimpleListWithoutLogin(tenantCode, tenantCodeHeader);
    }

    /**
     * 出网脱敏：服务层的完整 {@link StaffSimpleVO} → 对外最小视图 {@link PublicStaffVO}。
     *
     * <p>保留 {@code id} / {@code accountNo} / {@code name}（消费方
     * {@code TeamStaffAdapter#parseStaffResponse} 与人员下拉真正依赖的三个字段）；
     * 剔除 {@code telephone} / {@code jobNumber} 与 {@code extend} 里的
     * {@code email} / {@code userNo}（工号）—— 这四项是本次要止血的 PII。
     */
    private static List<PublicStaffVO> narrow(List<StaffSimpleVO> rows) {
        List<PublicStaffVO> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        for (StaffSimpleVO r : rows) {
            if (r == null) {
                continue;
            }
            PublicStaffVO vo = new PublicStaffVO();
            vo.setId(r.getId());
            vo.setAccountNo(r.getAccountNo());
            vo.setName(r.getName());
            vo.setExtend(safeExtend(r));
            out.add(vo);
        }
        return out;
    }

    /**
     * 挑出 {@code extend} 里**非 PII** 的子集，并保证**永不为空**。
     *
     * <p>空 extend 会被消费方整条丢弃（{@code if (extend == null || extend.isEmpty()) continue;}）
     * ⇒ 那样员工下拉会变成空列表。故这里至少写入 {@code nickname} 与 {@code flowerName}
     * （两者都回落自 {@code name}，而 {@code name} 在服务层已回落过 username ⇒ 非空）。
     */
    private static Map<String, Object> safeExtend(StaffSimpleVO r) {
        Map<String, Object> src = r.getExtend();
        Map<String, Object> out = new LinkedHashMap<>();

        String nickname = src == null ? null : asString(src.get("nickname"));
        String flowerName = src == null ? null : asString(src.get("flowerName"));
        if (isBlank(flowerName)) {
            flowerName = isBlank(nickname) ? r.getName() : nickname;
        }
        if (isBlank(nickname)) {
            nickname = r.getName();
        }
        out.put("nickname", nickname);
        out.put("flowerName", flowerName);
        Object avatar = src == null ? null : src.get("avatar");
        if (avatar != null) {
            out.put("avatar", avatar);
        }
        // ⚠ 刻意**不**输出 extend.email 与 extend.userNo（= 工号）：这两个字段曾可被公网匿名拖走。
        return out;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 对外名录条目（脱敏视图）。
     *
     * <p><b>刻意不持有</b> {@code telephone} / {@code jobNumber} 字段 ——
     * 不是「置 null」，而是**结构上不存在**，这样序列化后的 JSON 里连字段名都不会出现。
     */
    public static class PublicStaffVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 员工主键（Long，出网由 z-boot-jackson-starter 统一转字符串） */
        private Long id;

        /** 账号（映射自 username） */
        private String accountNo;

        /** 姓名（映射自 nickname，回落 username） */
        private String name;

        /** 非 PII 扩展字段（花名 / 昵称 / 头像）；永不返回 null */
        private Map<String, Object> extend = new LinkedHashMap<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public Map<String, Object> getExtend() {
            return extend;
        }

        public void setExtend(Map<String, Object> extend) {
            this.extend = extend == null ? new LinkedHashMap<>() : extend;
        }
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
