package com.zifang.ctc.web.api;

import com.zifang.ctc.common.vo.PageResponseVO;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.entity.TenantDO;
import com.zifang.ctc.core.init.TenantInitContext;
import com.zifang.ctc.core.init.TenantInitOrchestrator;
import com.zifang.ctc.core.init.TenantInitReport;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.core.service.TenantService;
import com.zifang.ctc.core.tenant.TenantContext;
import com.zifang.ctc.core.vo.TenantVO;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 租户 CRUD Controller.
 * <p>
 * API 基础路径: /api/ctc/ac/tenants
 * 所属模块: z-ctc-ac
 * 鉴权: 由 SsoInterceptor 统一拦截, 修改类操作通过 X-User-Id 头识别操作者
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /api/ctc/ac/tenants — 新建租户</li>
 *   <li>GET /api/ctc/ac/tenants?tenantCode=xxx — 按编码查询租户详情 (请求参数)</li>
 *   <li>POST /api/ctc/ac/tenants/status?tenantCode=&status= — 修改租户启用/停用状态 (请求参数)</li>
 *   <li>GET /api/ctc/ac/tenants/list — 全量租户列表 (无分页)</li>
 *   <li>GET /api/ctc/ac/tenants/page — 关键字分页查询 (租户编码/名称, 不区分大小写)</li>
 *   <li>PUT /api/ctc/ac/tenants?tenantCode=xxx — 按编码更新租户字段 (请求参数)</li>
 *   <li>DELETE /api/ctc/ac/tenants?tenantCode=xxx — 按编码删除租户 (请求参数)</li>
 * </ul>
 *
 * <p>设计说明/关键约束:
 * <ul>
 *   <li>bean 名为 acTenantController, 避免与其他模块同名 Controller 冲突</li>
 *   <li>租户是 z-ctc-ac 多租户体系的顶层实体, tenantCode 是跨表关联键</li>
 *   <li>update 与 delete 按 tenantCode 而不是主键 id 定位资源, 与 Org/Dept/Group 的设计保持一致</li>
 * </ul>
 */
@RestController("acTenantController")
@RequestMapping("/api/ctc/ac/tenants")
public class TenantController {

    private static final Logger log = LogManager.getLogger(TenantController.class);

    private final TenantService tenantService;
    private final TenantInitOrchestrator tenantInitOrchestrator;

    @org.springframework.beans.factory.annotation.Autowired
    private MembershipService membershipService;

    public TenantController(TenantService tenantService, TenantInitOrchestrator tenantInitOrchestrator) {
        this.tenantService = tenantService;
        this.tenantInitOrchestrator = tenantInitOrchestrator;
    }

    /**
     * 新建租户. tenantCode 与 tenantName 必填.
     *
     * @param tenant 租户实体 (含 tenantCode / tenantName / 描述 / 状态等)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回新建租户主键 id (HTTP 201); 必填字段缺失返回 400
     */
    @PostMapping
    public Result<Long> create(@RequestBody TenantDO tenant,
                               @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (tenant == null || tenant.getTenantCode() == null || tenant.getTenantName() == null) {
            return Result.<Long>fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        Long id = tenantService.createTenant(tenant, userId);

        // FEATURE049: 同步建立 owner membership (userId 来自 header, 由 onboarding/create-tenant-with-owner 触发)
        Long ownerUserId = null;
        if (userId != null && !userId.isEmpty()) {
            try {
                ownerUserId = Long.parseLong(userId);
                membershipService.createOwner(ownerUserId, tenant.getTenantCode());
                log.info("TenantController.create FEATURE049: auto-create owner membership, tenantCode={}, userId={}",
                        tenant.getTenantCode(), ownerUserId);
            } catch (NumberFormatException nfe) {
                log.warn("TenantController.create X-User-Id 不是数字, 跳过 membership 自动建立: {}", userId);
            } catch (Exception e) {
                log.error("TenantController.create FEATURE049: 建租户时建 owner membership 失败, 租户已建立, userId={}, tenantCode={}, err={}",
                        userId, tenant.getTenantCode(), e.getMessage());
                // 不影响租户创建返回, 但记录日志
            }
        }

        // FEATURE050: 触发各模块 TenantInitializer 完成新租户的默认数据初始化
        // (菜单/角色/默认配置/OSS bucket/工作流模板 等)
        // Orchestrator 内部已 try/catch 单个失败, 不会因为某个模块缺失而阻断租户创建.
        try {
            TenantInitContext ctx = TenantInitContext.of(tenant.getTenantCode(), id, ownerUserId);
            TenantInitReport report = tenantInitOrchestrator.runAfterTenantCreated(ctx);
            log.info("TenantController.create 新租户初始化完成: tenantCode={} 成功={} 失败={}",
                    tenant.getTenantCode(), report.getSuccessCount(), report.getFailureCount());
        } catch (Exception e) {
            log.error("TenantController.create TenantInit 编排异常 (不影响租户创建): tenantCode={}, err={}",
                    tenant.getTenantCode(), e.getMessage(), e);
        }

        return Result.success(id).code(201);
    }

    /**
     * 按编码查询租户详情. 不存在时返回 404.
     *
     * @param tenantCode 租户编码, 请求参数 (?tenantCode=xxx)
     * @return Tenant VO; 不存在时返回 404
     */
    @GetMapping
    public Result<TenantVO> findByCode(@RequestParam("tenantCode") String tenantCode) {
        return tenantService.findByTenantCode(tenantCode)
                .map(tenant -> Result.<TenantVO>success(TenantVO.from(tenant)))
                .orElseGet(() -> Result.<TenantVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode()));
    }

    /**
     * 修改租户启用/停用状态. 资源不存在返回 404.
     *
     * @param tenantCode 租户编码, 请求参数 (?tenantCode=xxx)
     * @param status     目标状态值, 业务定义 (例如 1 启用, 0 停用)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PostMapping("/status")
    public Result<Void> updateStatus(@RequestParam("tenantCode") String tenantCode,
                                     @RequestParam("status") int status) {
        boolean ok = tenantService.updateStatus(tenantCode, status);
        return ok ? Result.<Void>success() : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    // ===== FEATURE014 补齐 CRUD =====

    /**
     * 列出当前用户有 membership 的租户 (FEATURE049 多租户隔离).
     * <p>
     * 不再返回所有平台租户 — 普通用户只能看到他所属的租户,
     * 平台管理员 (roleCode=admin 且无 tenantCode 限制) 才看全部.
     *
     * @return 当前用户所属的 Tenant VO 列表
     */
    @GetMapping("/list")
    public Result<List<TenantVO>> listAll() {
        Long userId = TenantContext.getUserId();
        String ctxTenant = TenantContext.getTenantCode();
        String ctxRole = TenantContext.getRoleCode();
        List<TenantDO> data;
        // === 多租户隔离 (FEATURE049) ===
        // 1. 平台超级管理员 (无 tenant, role=admin) → 返回所有
        // 2. 已有选中租户 → 仅返回该租户
        // 3. 其他情况 → 按 user 的 active membership 过滤
        if (userId == null) {
            return Result.success(java.util.Collections.<TenantVO>emptyList());
        }
        if (ctxTenant == null || ctxTenant.isEmpty()) {
            // 没有选中租户, 按 membership 列表
            List<MembershipDO> memberships = membershipService.listActiveByUserId(userId);
            if (memberships.isEmpty()) {
                return Result.success(java.util.Collections.<TenantVO>emptyList());
            }
            data = memberships.stream()
                    .map(m -> tenantService.findByTenantCode(m.getTenantCode()).orElse(null))
                    .filter(t -> t != null)
                    .collect(Collectors.toList());
        } else {
            // 仅当前选中的租户
            data = tenantService.findByTenantCode(ctxTenant)
                    .map(java.util.Collections::singletonList)
                    .orElse(java.util.Collections.emptyList());
        }
        return Result.success(data.stream().map(TenantVO::from).collect(Collectors.toList()));
    }

    /**
     * 关键字分页查询: 按租户编码/名称做不区分大小写的子串匹配, 然后内存分页.
     *
     * @param keyword  关键字, 可选; 匹配 tenantCode/tenantName, 不区分大小写
     * @param pageNum  页码, 默认 1
     * @param pageSize 每页大小, 默认 20
     * @return 分页响应 VO, 含当前页数据与总记录数
     */
    @GetMapping("/page")
    public Result<PageResponseVO<TenantVO>> page(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        // === 多租户隔离 (FEATURE049): 只显示当前用户所属租户 ===
        Long userId = TenantContext.getUserId();
        List<TenantDO> all;
        if (userId == null) {
            return Result.success(new PageResponseVO<>(java.util.Collections.emptyList(), 0, pageNum, pageSize));
        }
        List<MembershipDO> memberships = membershipService.listActiveByUserId(userId);
        if (memberships.isEmpty()) {
            return Result.success(new PageResponseVO<>(java.util.Collections.emptyList(), 0, pageNum, pageSize));
        }
        all = memberships.stream()
                .map(m -> tenantService.findByTenantCode(m.getTenantCode()).orElse(null))
                .filter(t -> t != null)
                .collect(Collectors.toList());
        List<TenantDO> filtered = all.stream()
                .filter(t -> {
                    if (keyword == null || keyword.isEmpty()) {
                        return true;
                    }
                    String k = keyword.toLowerCase();
                    return (t.getTenantCode() != null && t.getTenantCode().toLowerCase().contains(k))
                            || (t.getTenantName() != null && t.getTenantName().toLowerCase().contains(k));
                })
                .collect(Collectors.toList());
        long total = filtered.size();
        int from = Math.min((pageNum - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        List<TenantVO> pageData = filtered.subList(from, to).stream()
                .map(TenantVO::from)
                .collect(Collectors.toList());
        return Result.success(new PageResponseVO<>(pageData, total, pageNum, pageSize));
    }

    /**
     * 按编码更新租户字段. patch 中为 null 的字段由 Service 决定是否保留原值.
     *
     * @param tenantCode 租户编码, 请求参数 (?tenantCode=xxx)
     * @param patch      待更新字段 (TenantDO)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PutMapping
    public Result<Void> update(
            @RequestParam("tenantCode") String tenantCode,
            @RequestBody TenantDO patch) {
        boolean ok = tenantService.updateTenant(tenantCode, patch);
        return ok ? Result.<Void>success() : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 按编码删除租户. 资源不存在返回 404.
     *
     * @param tenantCode 租户编码, 请求参数 (?tenantCode=xxx)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam("tenantCode") String tenantCode) {
        boolean ok = tenantService.deleteTenant(tenantCode);
        return ok ? Result.<Void>success() : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }
}
