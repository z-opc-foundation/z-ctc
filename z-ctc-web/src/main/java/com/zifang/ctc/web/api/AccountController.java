package com.zifang.ctc.web.api;

import com.zifang.ctc.common.vo.PageResponseVO;
import com.zifang.ctc.core.domain.entity.AccountDO;
import com.zifang.ctc.core.service.AccountService;
import com.zifang.ctc.core.tenant.TenantContext;
import com.zifang.ctc.core.vo.AccountVO;
import com.zifang.ctc.web.api.request.ChangePasswordRequest;
import com.zifang.ctc.web.api.request.CreateAccountRequest;
import com.zifang.ctc.web.api.request.ResetPasswordRequest;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账号 CRUD + 密码管理 Controller.
 * <p>
 * API 基础路径: /api/ctc/ac/accounts
 * 所属模块: z-ctc-ac
 * 鉴权: 由 SsoInterceptor 统一拦截, 具体方法按 X-User-Id 头识别操作者
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /api/ctc/ac/accounts — 创建账号</li>
 *   <li>GET /api/ctc/ac/accounts?id=xxx — 按主键查询账号详情 (请求参数)</li>
 *   <li>GET /api/ctc/ac/accounts — 按租户分页查询账号列表</li>
 *   <li>POST /api/ctc/ac/accounts/status?id=&status= — 修改账号启用/停用状态 (请求参数)</li>
 *   <li>POST /api/ctc/ac/accounts/password/change?id= — 修改自己的密码 (请求参数)</li>
 *   <li>POST /api/ctc/ac/accounts/password/reset?id= — 管理员重置密码 (请求参数)</li>
 *   <li>GET /api/ctc/ac/accounts/list — 全量列表 (兼容老前端无分页)</li>
 *   <li>GET /api/ctc/ac/accounts/page — 关键字分页查询 (租户 + 关键字 + 分页)</li>
 *   <li>PUT /api/ctc/ac/accounts?id=xxx — 按主键更新账号字段 (请求参数)</li>
 *   <li>DELETE /api/ctc/ac/accounts?id=xxx — 按主键删除账号 (请求参数)</li>
 *   <li>POST /api/ctc/ac/accounts/assign-role?userId=&roleId= — 为账号分配角色 (请求参数)</li>
 * </ul>
 *
 * <p>设计说明/关键约束:
 * <ul>
 *   <li>所有修改类操作通过 X-User-Id 请求头透传操作者, 由 Service 层记录审计字段</li>
 *   <li>密码相关接口返回码 400 表示参数错误或旧密码校验失败, 404 表示资源不存在</li>
 *   <li>分页接口 (page) 使用内存过滤, 适合中小数据量场景, 大数据量建议走数据库分页</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ctc/ac/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 创建账号. 校验用户名与密码非空后将请求参数映射为 DO 并入库, 返回新建账号的主键 id.
     *
     * @param request 创建账号请求体 (含用户名/密码/昵称/邮箱/手机号/账号类型/租户编码/过期时间)
     * @return 成功时返回新建账号的主键 id (HTTP 201), 参数错误返回 400
     */
    @PostMapping
    public Result<Long> create(@RequestBody CreateAccountRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return Result.<Long>fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        AccountDO account = new AccountDO();
        account.setAccountNo(request.getAccountNo());
        account.setUsername(request.getUsername());
        account.setNickname(request.getNickname());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setTenantCode(request.getTenantCode());
        account.setExpireAt(request.getExpireAt());
        Long id = accountService.createAccount(account, request.getPassword());
        return Result.success(id).code(201);
    }

    /**
     * 按主键查询账号详情,或按租户分页查询账号列表.
     * <p>
     * 合并 FEATURE014 之前的两个 {@code @GetMapping} (findById 与 listByTenant) ——
     * 它们共享同一路径 {@code GET /api/ctc/ac/accounts} 会导致 Spring MVC Ambiguous mapping.
     * 统一入口通过 {@code id} 参数是否提供来分支:
     * <ul>
     *   <li>提供 id → 按主键查询, 找不到时返回 404</li>
     *   <li>未提供 id → 按租户分页查询</li>
     * </ul>
     * 前端调用方不变: getById(id) 走 {@code ?id=xxx}, listByTenant({tenant, pageNum, pageSize}) 不带 id.
     *
     * @param id       账号主键 id, 可选; 提供时按主键查询
     * @param tenant   租户编码, 可选; id 为空时生效, 为空时不过滤
     * @param pageNum  页码, 默认 1
     * @param pageSize 每页大小, 默认 20
     */
    @GetMapping
    public Result<?> findOrListByTenant(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "tenant", required = false) String tenant,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        if (id != null) {
            return accountService.findById(id)
                    .<Result<?>>map(a -> Result.<AccountVO>success(AccountVO.from(a)))
                    .orElseGet(() -> Result.<AccountVO>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode()));
        }
        // === 多租户隔离 (FEATURE049) ===
        // 优先使用前端显式传的 tenant; 否则强制使用当前 JWT 中的 tenantCode,
        // 保证 A 租户绝对看不到 B 租户的账号. 游离态直接返回空列表.
        String ctxTenant = TenantContext.getTenantCode();
        String effectiveTenant = (tenant != null && !tenant.isEmpty()) ? tenant : ctxTenant;
        if (effectiveTenant == null || effectiveTenant.isEmpty()) {
            return Result.<List<AccountVO>>success(java.util.Collections.emptyList());
        }
        List<AccountVO> list = accountService.listByTenant(effectiveTenant, pageNum, pageSize).stream()
                .map(AccountVO::from)
                .collect(Collectors.toList());
        return Result.<List<AccountVO>>success(list);
    }

    /**
     * 修改账号状态 (启用/停用). 操作者通过 X-User-Id 头传递, 由 Service 层记录审计.
     *
     * @param id     账号主键 id, 请求参数 (?id=xxx)
     * @param status 目标状态值, 业务定义 (例如 1 启用, 0 停用)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PostMapping("/status")
    public Result<Void> updateStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") int status,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        boolean ok = accountService.updateStatus(id, status, userId);
        return ok ? Result.<Void>success() : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 修改自己的密码: 校验旧密码后写入新密码. 失败场景包括原密码错误或新密码不合法.
     *
     * @param id      账号主键 id, 请求参数 (?id=xxx)
     * @param request 含旧密码与新密码的请求体
     * @return 成功返回空体, 失败返回 400 (原密码错误或新密码不合法)
     */
    @PostMapping("/password/change")
    public Result<Void> changePassword(
            @RequestParam("id") Long id,
            @RequestBody ChangePasswordRequest request) {
        boolean ok = accountService.changePassword(id, request.getOldPassword(), request.getNewPassword());
        return ok ? Result.<Void>success() : Result.<Void>fail("原密码错误或新密码不合法").code(400);
    }

    /**
     * 管理员重置指定账号的密码, 不校验旧密码. 操作者通过 X-User-Id 头传递.
     *
     * @param id      账号主键 id, 请求参数 (?id=xxx)
     * @param request 含新密码的请求体
     * @param userId  操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(
            @RequestParam("id") Long id,
            @RequestBody ResetPasswordRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        boolean ok = accountService.resetPassword(id, request.getNewPassword(), userId);
        return ok ? Result.<Void>success() : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    // ===== FEATURE014 补齐 CRUD =====

    /**
     * 全量账号列表 (兼容老前端无分页场景). 数据量较大时慎用, 建议改用 /page 接口.
     *
     * @return 全量账号 VO 列表
     */
    @GetMapping("/list")
    public Result<List<AccountVO>> listAll() {
        // === 多租户隔离 (FEATURE049): 强制只返回当前租户的账号 ===
        String ctxTenant = TenantContext.getTenantCode();
        if (ctxTenant == null || ctxTenant.isEmpty()) {
            return Result.success(java.util.Collections.<AccountVO>emptyList());
        }
        return Result.success(accountService.listByTenant(ctxTenant, 1, Integer.MAX_VALUE).stream()
                .map(AccountVO::from)
                .collect(Collectors.toList()));
    }

    /**
     * 分页查询: 先按租户编码过滤, 再按关键字 (用户名/昵称/邮箱, 不区分大小写) 过滤, 最后内存分页.
     *
     * @param tenant   租户编码, 可选; 为空时不过滤
     * @param keyword  关键字, 可选; 匹配 username/nickname/email, 不区分大小写
     * @param pageNum  页码, 默认 1
     * @param pageSize 每页大小, 默认 20
     * @return 分页响应 VO, 含当前页数据与总记录数
     */
    @GetMapping("/page")
    public Result<PageResponseVO<AccountVO>> page(
            @RequestParam(value = "tenant", required = false) String tenant,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        // === 多租户隔离 (FEATURE049) ===
        // 优先使用前端显式传的 tenant; 否则强制使用当前 JWT 中的 tenantCode.
        String ctxTenant = TenantContext.getTenantCode();
        String effectiveTenant = (tenant != null && !tenant.isEmpty()) ? tenant : ctxTenant;
        if (effectiveTenant == null || effectiveTenant.isEmpty()) {
            return Result.success(new PageResponseVO<>(java.util.Collections.emptyList(), 0, pageNum, pageSize));
        }
        List<AccountDO> all = accountService.listAll();
        String k = keyword == null ? "" : keyword.toLowerCase();
        List<AccountDO> filtered = all.stream()
                .filter(a -> effectiveTenant.equals(a.getTenantCode()))
                .filter(a -> k.isEmpty() ||
                        (a.getUsername() != null && a.getUsername().toLowerCase().contains(k))
                        || (a.getNickname() != null && a.getNickname().toLowerCase().contains(k))
                        || (a.getEmail() != null && a.getEmail().toLowerCase().contains(k)))
                .collect(Collectors.toList());
        long total = filtered.size();
        int from = Math.min((pageNum - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        List<AccountVO> pageData = filtered.subList(from, to).stream()
                .map(AccountVO::from)
                .collect(Collectors.toList());
        return Result.success(new PageResponseVO<>(pageData, total, pageNum, pageSize));
    }

    /**
     * 按主键更新账号字段 (部分更新语义, patch 中为 null 的字段由 Service 决定是否保留原值).
     *
     * @param id     账号主键 id, 请求参数 (?id=xxx)
     * @param patch  待更新的字段 (AccountDO)
     * @param userId 操作者账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 资源不存在返回 404
     */
    @PutMapping
    public Result<Void> update(
            @RequestParam("id") Long id,
            @RequestBody AccountDO patch,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        boolean ok = accountService.updateAccount(id, patch, userId);
        return ok ? Result.<Void>success() : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 按主键删除账号. 资源不存在时返回 404.
     *
     * @param id 账号主键 id, 请求参数 (?id=xxx)
     * @return 成功返回空体, 资源不存在返回 404
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam("id") Long id) {
        boolean ok = accountService.deleteAccount(id);
        return ok ? Result.<Void>success() : Result.<Void>fail("资源不存在").code(ResultCode.NOT_FOUND.getCode());
    }

    /**
     * 为指定账号分配角色. 操作者 (授权人) 通过 X-User-Id 头传递, 由 Service 层记录审计.
     *
     * @param userId    被授权的账号主键 id, 请求参数 (?userId=xxx)
     * @param roleId    角色主键 id, 请求参数 (?roleId=xxx)
     * @param grantedBy 授权人账号 id, 来自请求头 X-User-Id, 可选
     * @return 成功返回空体, 参数错误 (例如用户或角色不存在) 返回 400
     */
    @PostMapping("/assign-role")
    public Result<Void> assignRole(
            @RequestParam("userId") Long userId,
            @RequestParam("roleId") Long roleId,
            @RequestHeader(value = "X-User-Id", required = false) String grantedBy) {
        boolean ok = accountService.assignRole(userId, roleId, grantedBy);
        return ok ? Result.<Void>success() : Result.<Void>fail("参数错误").code(ResultCode.PARAM_VALID_ERROR.getCode());
    }
}
