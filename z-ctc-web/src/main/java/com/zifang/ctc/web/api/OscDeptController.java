package com.zifang.ctc.web.api;

import com.zifang.util.core.meta.BaseStatusCode;
import com.zifang.util.core.meta.Result;
import com.zifang.ctc.core.service.DeptTreeService;
import com.zifang.ctc.core.vo.DeptTreeVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 人事架构组织树 Controller（{@code /ctc-osc} 前缀）。
 *
 * <p><b>2026-10-04 新增</b>。z-opc 侧 {@code TeamStaffAdapter} 一直在调用
 * {@code POST /ctc-osc/dept/getStaffDeptTree} 与
 * {@code POST /ctc-osc/staff/getStaffSimpleListWithoutLogin}，但这两个端点
 * <b>本仓从未定义</b>（全 40 仓搜索只命中"被调用"、无任何"定义"）——
 * 对外表现为组织树接口返回 200 但数据恒空。
 *
 * <p><b>为什么单独开 {@code /ctc-osc} 而不是并入 {@code /api/ctc/ac}</b>：
 * {@code /api/ctc/ac} 是本仓既有的管理面 CRUD（需登录、按 tenant/domain 精确寻址），
 * 而消费方是<b>服务端到服务端</b>的调用（带 Cookie、按 orgId 寻址）。
 * 两者鉴权模型与调用方都不同，混在一起会让 {@code SsoInterceptor} 的
 * {@code exclude-paths} 难以精确放行。
 *
 * <p><b>响应契约</b>：{@code {code:0, data:[{id,name,parentId,childrenList,staffInfos}]}}。
 * 字段名与消费方 {@code TeamCtcOrgResolver} 的三 key 兼容口径对齐
 * （{@code childrenList}→{@code children}、{@code staffInfos}→{@code staffList}→{@code staffIds}）。
 *
 * <p><b>雪花 ID</b>：{@code id}/{@code parentId}/{@code staffInfos[].id} 为 Long，
 * 出网由 z-boot-jackson-starter 统一转字符串，此处不做手工转换。
 *
 * @author zifang
 * @since 1.0.0
 */
@RestController("ctcOscDeptController")
@RequestMapping("/ctc-osc/dept")
public class OscDeptController {

    private static final Logger log = LogManager.getLogger(OscDeptController.class);

    private final DeptTreeService deptTreeService;

    public OscDeptController(DeptTreeService deptTreeService) {
        this.deptTreeService = deptTreeService;
    }

    /**
     * 获取人事架构部门树（含直属人员）。
     *
     * <p><b>寻址兼容性</b>（消费方实测口径）：调用方
     * {@code TeamStaffAdapter#fetchOrgTree} 的请求形态是
     * <ul>
     *   <li>请求体：{@code {"orgId": <long>, "containPositionDept": true}}</li>
     *   <li>请求头：{@code Tenant-Code: <tenantCode>}</li>
     * </ul>
     * 但本仓数据按 {@code (tenant_code, domain_code)} 二元组寻址
     * （{@code z_ctc_ac_dept} 的唯一键就是 {@code uk_tenant_domain_dept}），
     * 没有 orgId 维度。
     * <p>因此取租户的优先级为：<b>query tenantCode → {@code Tenant-Code} 头 → {@code default}</b>。
     * 域编码消费方不传，缺省 {@code default}（与 z-opc 侧
     * {@code sso.domain-overrides[*].tenantCode=default} 同源）。
     * <p>这样无论消费方将来是发 orgId 还是补传 tenantCode，本端点都能工作。
     *
     * @param tenantCode 租户编码；缺省时读 {@code Tenant-Code} 头，再缺省 {@code default}
     * @param domainCode 域编码；缺省 {@code default}
     * @param requestBody 消费方请求体（可选，含 orgId / containPositionDept）
     * @return 部门树；无数据返回空数组
     */
    @PostMapping("/getStaffDeptTree")
    public Result<List<DeptTreeVO>> getStaffDeptTree(
            @RequestParam(required = false, defaultValue = "") String tenantCode,
            @RequestParam(required = false, defaultValue = "default") String domainCode,
            @RequestHeader(value = "Tenant-Code", required = false) String tenantCodeHeader,
            @RequestBody(required = false) java.util.Map<String, Object> requestBody) {
        try {
            String tenant = resolveTenantCode(tenantCode, tenantCodeHeader);
            List<DeptTreeVO> tree = deptTreeService.buildDeptTree(tenant, domainCode);
            if (tree.isEmpty()) {
                log.info("[OscDeptController] 部门树为空: tenant={}, domain={}, req={}",
                        tenant, domainCode, requestBody);
            }
            return Result.success(tree);
        } catch (IllegalArgumentException e) {
            return Result.error(BaseStatusCode.PARAM_VALID_EXCEPTION, e.getMessage());
        } catch (Exception e) {
            log.warn("[OscDeptController] getStaffDeptTree 失败: {}", e.toString());
            return Result.error(BaseStatusCode.FAIL, "查询部门树失败: " + e.getMessage());
        }
    }

    /** 租户编码优先级：query 参数 → Tenant-Code 头 → {@code default}。 */
    private static String resolveTenantCode(String fromQuery, String fromHeader) {
        if (fromQuery != null && !fromQuery.trim().isEmpty()) {
            return fromQuery.trim();
        }
        if (fromHeader != null && !fromHeader.trim().isEmpty()) {
            return fromHeader.trim();
        }
        return "default";
    }

    /**
     * 获取扁平部门列表（不组树），供只需列表不需要层级的调用方。
     *
     * @return 与 {@link #getStaffDeptTree} 同一数据源，展开为深度优先的平铺序列
     */
    @PostMapping("/getStaffDeptList")
    public Result<List<DeptTreeVO>> getStaffDeptList(
            @RequestParam(required = false, defaultValue = "") String tenantCode,
            @RequestParam(required = false, defaultValue = "default") String domainCode,
            @RequestHeader(value = "Tenant-Code", required = false) String tenantCodeHeader) {
        try {
            List<DeptTreeVO> flat = new java.util.ArrayList<>();
            for (DeptTreeVO root : deptTreeService.buildDeptTree(
                    resolveTenantCode(tenantCode, tenantCodeHeader), domainCode)) {
                flatten(root, flat);
            }
            return Result.success(flat);
        } catch (IllegalArgumentException e) {
            return Result.error(BaseStatusCode.PARAM_VALID_EXCEPTION, e.getMessage());
        } catch (Exception e) {
            log.warn("[OscDeptController] getStaffDeptList 失败: {}", e.toString());
            return Result.error(BaseStatusCode.FAIL, "查询部门列表失败: " + e.getMessage());
        }
    }

    /** 深度优先展开，childrenList 不置空以便调用方自行判断层级。 */
    private static void flatten(DeptTreeVO node, List<DeptTreeVO> out) {
        if (node == null) {
            return;
        }
        out.add(node);
        for (DeptTreeVO child : node.getChildrenList()) {
            flatten(child, out);
        }
    }

    /**
     * 健康检查。
     *
     * @return 固定字段，用于确认该前缀的 controller 已被 Spring 注册
     */
    @PostMapping("/health")
    public Result<?> health() {
        return Result.success(Collections.singletonMap("module", "z-ctc-osc"));
    }
}
