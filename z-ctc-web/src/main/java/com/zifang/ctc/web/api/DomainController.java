package com.zifang.ctc.web.api;

import com.zifang.ctc.common.vo.PageResponseVO;
import com.zifang.ctc.core.domain.entity.DomainDO;
import com.zifang.ctc.core.service.DomainService;
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
 * 鉴权: 由 SsoInterceptor 统一拦截, 修改类操作通过 X-User-Id 头识别操作者
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
 *   <li>listByTenant 必须传 tenantCode, 否则返回参数错误</li>
 *   <li>create 接口对状态异常 (如重名) 返回 409, 参数错误返回 400</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ctc/ac/domains")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    /**
     * 按租户编码查询该租户下所有域. tenantCode 必填.
     *
     * @param tenantCode 租户编码, 必填
     * @return 该租户下的域 VO 列表; tenantCode 为空时返回 400
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
        List<DomainDO> data = (ctxTenant == null || ctxTenant.isEmpty())
                ? java.util.Collections.<DomainDO>emptyList()
                : domainService.listByTenant(ctxTenant);
        List<DomainVO> voList = data.stream().map(DomainVO::from).collect(Collectors.toList());
        return Result.success(new PageResponseVO<>(voList, data.size(), 1, data.size()));
    }

    /**
     * 按主键查询域详情. 不存在时返回 404.
     *
     * @param id 域主键 id, 请求参数 (?id=xxx)
     * @return 域 VO; 不存在时返回失败结果 (404)
     */
    @GetMapping
    public Result<DomainVO> getById(@RequestParam("id") Long id) {
        return domainService.findById(id)
                .map(d -> Result.<DomainVO>success(DomainVO.from(d)))
                .orElseGet(() -> Result.<DomainVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode()));
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
        return domainService.deleteDomain(id)
                ? Result.<Void>success()
                : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }
}
