package com.zifang.ctc.web.api;

import com.zifang.ctc.common.vo.PageResponseVO;
import com.zifang.ctc.core.domain.entity.DomainDO;
import com.zifang.ctc.core.service.DomainService;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.core.service.PlatformAdminService;
import com.zifang.ctc.core.tenant.TenantContext;
import com.zifang.ctc.core.vo.DomainVO;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 域 (Domain) CRUD Controller.
 * <p>
 * API 基础路径: /api/ctc/ac/domains
 * 所属模块: z-ctc-ac
 * 鉴权: 登录态由 SsoInterceptor 统一拦截; 租户归属在方法内自判 —
 * 读 (list/list-all/get) 要求平台管理员或该租户成员, 写 (create/update/delete)
 * 要求平台管理员或该租户的 owner/admin. 修改类操作通过 X-User-Id 头记录操作者.
 *
 * <p>主要端点:
 * <ul>
 *   <li>GET /api/ctc/ac/domains/list — 按租户编码查询域列表</li>
 *   <li>GET /api/ctc/ac/domains/list-all — 全量域列表 (无分页, 一次返回)</li>
 *   <li>GET /api/ctc/ac/domains?id=xxx — 按主键查询域详情 (请求参数)</li>
 *   <li>POST /api/ctc/ac/domains — 新建域</li>
 *   <li>PUT /api/ctc/ac/domains?id=xxx — 按主键更新域字段 (请求参数)</li>
 *   <li>DELETE /api/ctc/ac/domains?id=xxx — 按主键删除域 (请求参数)</li>
 * </ul>
 *
 * <p>设计说明/关键约束:
 * <ul>
 *   <li>bean 名为 acDomainController, 与 z-ctc-authz 中可能存在的同名 Controller 区分</li>
 *   <li>listByTenant 不传 tenantCode 时取当前 JWT 租户, 两者都没有则返回空列表</li>
 *   <li>create 接口对状态异常 (如重名) 返回 409, 参数错误返回 400</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ctc/ac/domains")
public class DomainController {

    private final DomainService domainService;
    private final MembershipService membershipService;
    private final PlatformAdminService platformAdminService;

    public DomainController(DomainService domainService,
                            MembershipService membershipService,
                            PlatformAdminService platformAdminService) {
        this.domainService = domainService;
        this.membershipService = membershipService;
        this.platformAdminService = platformAdminService;
    }

    /**
     * 平台管理员: 不受租户边界约束, 可管任意租户的域.
     */
    private boolean isPlatformAdmin() {
        return platformAdminService.isPlatformAdmin(TenantContext.getUserId());
    }

    /**
     * 能看该租户的域 = 平台管理员, 或该租户的活跃成员.
     */
    private boolean canRead(String tenantCode) {
        return isPlatformAdmin() || membershipService.isMember(TenantContext.getUserId(), tenantCode);
    }

    /**
     * 能改该租户的域 = 平台管理员, 或该租户的 owner/admin.
     */
    private boolean canManage(String tenantCode) {
        return isPlatformAdmin() || membershipService.isTenantAdmin(TenantContext.getUserId(), tenantCode);
    }

    /**
     * 构造 403 拒绝响应体.
     */
    private <T> Result<T> forbidden() {
        return Result.<T>fail("无权操作该租户的域").code(ResultCode.REQ_REJECT.getCode());
    }

    /**
     * 按租户编码查询该租户下所有域. 不传 tenantCode 时取当前 JWT 租户.
     *
     * @param tenantCode 租户编码, 可选
     * @return 该租户下的域 VO 列表; 无可用租户或无权访问时返回空/403
     */
    @GetMapping("/list")
    public Result<List<DomainVO>> listByTenant(
            @RequestParam(value = "tenantCode", required = false) String tenantCode) {
        // === 多租户隔离 (FEATURE049): 强制使用当前 JWT 中的 tenantCode ===
        String ctxTenant = TenantContext.getTenantCode();
        String effectiveTenant = (tenantCode != null && !tenantCode.isEmpty()) ? tenantCode : ctxTenant;
        if (effectiveTenant == null || effectiveTenant.isEmpty()) {
            return Result.success(java.util.Collections.<DomainVO>emptyList());
        }
        if (!canRead(effectiveTenant)) {
            return forbidden();
        }
        return Result.success(domainService.listByTenant(effectiveTenant).stream()
                .map(DomainVO::from)
                .collect(Collectors.toList()));
    }

    /**
     * 全量域列表, 不分页. 用 PageResponseVO 包装以兼容前端表格组件.
     * <p>
     * 多租户隔离: 仅返回当前 JWT tenantCode 下的域.
     *
     * @return 包装为单页 (pageNum=1, pageSize=总数) 的域分页响应
     */
    @GetMapping("/list-all")
    public Result<PageResponseVO<DomainVO>> listAll() {
        String ctxTenant = TenantContext.getTenantCode();
        if (ctxTenant == null || ctxTenant.isEmpty() || !canRead(ctxTenant)) {
            return Result.success(new PageResponseVO<>(java.util.Collections.<DomainVO>emptyList(), 0, 1, 0));
        }
        List<DomainDO> data = domainService.listByTenant(ctxTenant);
        List<DomainVO> voList = data.stream().map(DomainVO::from).collect(Collectors.toList());
        return Result.success(new PageResponseVO<>(voList, data.size(), 1, data.size()));
    }

    /**
     * 按主键查询域详情. 不存在时返回 404.
     *
     * @param id 域主键 id, 请求参数 (?id=xxx)
     * @return 域 VO; 不存在返回 404, 无权访问返回 403
     */
    @GetMapping
    public Result<DomainVO> getById(@RequestParam("id") Long id) {
        DomainDO domain = domainService.findById(id).orElse(null);
        if (domain == null) {
            return Result.<DomainVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
        }
        if (!canRead(domain.getTenantCode())) {
            return forbidden();
        }
        return Result.success(DomainVO.from(domain));
    }

    /**
     * 新建域. 业务异常 (如编码冲突) 返回 409, 参数错误返回 400.
     *
     * @param domain 域实体 (含编码/名称/租户等)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功时返回新建域主键 id (HTTP 201); 状态冲突 409; 参数错误 400
     */
    @PostMapping
    public Result<Long> create(@RequestBody DomainDO domain,
                               @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (domain == null) {
            return Result.<Long>fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        // 目标租户: 请求指定优先, 否则落到当前 JWT 租户
        if (domain.getTenantCode() == null || domain.getTenantCode().isEmpty()) {
            domain.setTenantCode(TenantContext.getTenantCode());
        }
        if (!canManage(domain.getTenantCode())) {
            return forbidden();
        }
        try {
            Long id = domainService.createDomain(domain, userId);
            return Result.<Long>success(id).code(201);
        } catch (IllegalStateException e) {
            return Result.<Long>fail(e.getMessage()).code(409);
        } catch (IllegalArgumentException e) {
            return Result.<Long>fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
    }

    /**
     * 按主键更新域字段. 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.
     *
     * @param id     域主键 id, 请求参数 (?id=xxx)
     * @param patch  待更新字段 (DomainDO)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PutMapping
    public Result<Void> update(@RequestParam("id") Long id,
                               @RequestBody DomainDO patch,
                               @RequestHeader(value = "X-User-Id", required = false) String userId) {
        DomainDO domain = domainService.findById(id).orElse(null);
        if (domain == null) {
            return Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
        }
        if (!canManage(domain.getTenantCode())) {
            return forbidden();
        }
        return domainService.updateDomain(id, patch, userId)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 按主键删除域. 资源不存在时返回 404.
     *
     * @param id 域主键 id, 请求参数 (?id=xxx)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam("id") Long id) {
        DomainDO domain = domainService.findById(id).orElse(null);
        if (domain == null) {
            return Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
        }
        if (!canManage(domain.getTenantCode())) {
            return forbidden();
        }
        return domainService.deleteDomain(id)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }
}
