package com.zifang.ctc.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.ctc.core.domain.entity.LoginLogDO;
import com.zifang.ctc.core.domain.mapper.LoginLogMapper;
import com.zifang.ctc.core.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录日志查询 Controller.
 * <p>
 * FEATURE012: 补齐 z-ctc-ac 4A 5 域中的"登录日志"域
 * <p>
 * API 基础路径: /api/ctc/ac/login-log
 * 所属模块: z-ctc-ac
 * 鉴权: 由 SsoInterceptor 统一拦截 (只读接口, 无角色细化控制)
 *
 * <p>主要端点:
 * <ul>
 *   <li>GET /api/ctc/ac/login-log/list — 条件分页查询登录日志 (identifier 模糊 + tenantCode 精确)</li>
 *   <li>GET /api/ctc/ac/login-log/recent — 最新 10 条登录日志 (按 login_time 倒序)</li>
 * </ul>
 *
 * <p>设计说明/关键约束:
 * <ul>
 *   <li>bean 名为 acLoginLogController, 避免与其他模块同名 Controller 冲突</li>
 *   <li>登录日志数据由 z-ctc-ac 的 AuthnController 在登录成功/失败时写入 (recordLoginLog), 本 Controller 只做查询</li>
 *   <li>返回结构为裸 Map (与 Result 包装不同), 前端直接消费 total/list/page/size 字段</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ctc/ac/login-log")
public class LoginLogController {

    private final LoginLogMapper loginLogMapper;

    public LoginLogController(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    /**
     * 条件分页查询登录日志. 按 identifier 模糊匹配、tenantCode 精确匹配, login_time 倒序.
     *
     * @param page       页码, 默认 1
     * @param size       每页大小, 默认 20
     * @param identifier 登录标识 (用户名/手机号/邮箱 等), 可选; 非空时按 LIKE 过滤
     * @param tenantCode 租户编码, 可选; 非空时按等值过滤
     * @return 含 total / list / page / size 四个键的 Map
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String tenantCode) {
        QueryWrapper<LoginLogDO> wrapper = new QueryWrapper<>();
        if (identifier != null && !identifier.isEmpty()) {
            wrapper.like("identifier", identifier);
        }
        // === 多租户隔离 (FEATURE049): 优先使用前端传入的 tenantCode, 否则强制使用 JWT 上下文中的租户. ===
        String ctxTenant = TenantContext.getTenantCode();
        String effectiveTenant = (tenantCode != null && !tenantCode.isEmpty()) ? tenantCode : ctxTenant;
        if (effectiveTenant == null || effectiveTenant.isEmpty()) {
            // 游离态返回空 (不允许跨租户查看)
            Map<String, Object> empty = new HashMap<>();
            empty.put("total", 0);
            empty.put("list", java.util.Collections.emptyList());
            empty.put("page", page);
            empty.put("size", size);
            return empty;
        }
        wrapper.eq("tenant_code", effectiveTenant);
        wrapper.orderByDesc("login_time");
        Page<LoginLogDO> p = Page.of(page, size);
        Page<LoginLogDO> result = loginLogMapper.selectPage(p, wrapper);

        Map<String, Object> resp = new HashMap<>();
        resp.put("total", result.getTotal());
        resp.put("list", result.getRecords());
        resp.put("page", page);
        resp.put("size", size);
        return resp;
    }

    /**
     * 查询最新 10 条登录日志 (按 login_time 倒序), 用于首页/dashboard 实时活动流.
     *
     * @return 最新 10 条 LoginLogDO 列表
     */
    @GetMapping("/recent")
    public List<LoginLogDO> recent() {
        // === 多租户隔离 (FEATURE049): 仅返回当前租户的最近登录日志 ===
        String ctxTenant = TenantContext.getTenantCode();
        QueryWrapper<LoginLogDO> wrapper = new QueryWrapper<LoginLogDO>()
                .orderByDesc("login_time")
                .last("LIMIT 10");
        if (ctxTenant != null && !ctxTenant.isEmpty()) {
            wrapper.eq("tenant_code", ctxTenant);
        } else {
            // 游离态: 不允许返回任何日志
            return java.util.Collections.emptyList();
        }
        return loginLogMapper.selectList(wrapper);
    }

    /**
     * RESTful alias for FE makeApi('ctc/ac/login-log').page().
     * <p>{@code GET /api/ctc/ac/login-log/page?pageNum=&pageSize=&identifier=&tenantCode=}</p>
     * <p>FE uses {@code pageNum/pageSize}; the canonical endpoint uses {@code page/size}, so we
     * expose this thin alias to keep the makeApi pattern working.</p>
     */
    @GetMapping("/page")
    public Map<String, Object> pageAlias(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String tenantCode) {
        return list(pageNum, pageSize, identifier, tenantCode);
    }
}
