package com.zifang.ctc.web.api;

import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.mapper.AccountMapper;
import com.zifang.ctc.core.domain.mapper.MembershipMapper;
import com.zifang.ctc.core.domain.mapper.UserMapper;
import com.zifang.ctc.core.service.InvitationService;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.web.api.response.InvitationAcceptResult;
import com.zifang.ctc.web.api.response.InvitationCreateResult;
import com.zifang.ctc.web.api.response.InvitationRedeemResult;
import com.zifang.util.core.meta.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * FEATURE049: 邀请 Controller.
 * <p>
 * 端点:
 * POST /api/ctc/ac/invitations       发送邀请 (owner/admin 权限)
 * GET  /api/ctc/ac/invitations       查邀请列表
 * POST /api/ctc/ac/invitations/accept  接受邀请 (链接 token, 临时 token)
 * POST /api/ctc/ac/invitations/redeem   兑换邀请码 (临时 token)
 * POST /api/ctc/ac/invitations/{id}/revoke  撤销邀请
 */
@Tag(name = "4A-邀请 (FEATURE049)")
@RestController
@RequestMapping("/api/ctc/ac/invitations")
public class InvitationController {

    private static final Logger log = LogManager.getLogger(InvitationController.class);

    @Autowired
    private MembershipMapper membershipMapper;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private com.zifang.ctc.core.domain.mapper.InvitationMapper invitationMapper;
    @Autowired
    private InvitationService invitationService;
    @Autowired
    private MembershipService membershipService;

    /**
     * 发送邀请.
     * 重构: 改用 InvitationService.create (业务层管状态/过期/编码生成).
     */
    @Operation(summary = "发送邀请 (链接 / 码)")
    @PostMapping
    public Result<InvitationCreateResult> create(@RequestBody CreateInvitationRequest req,
                                              HttpServletRequest httpRequest) {
        if (req == null || req.getTenantCode() == null) {
            return Result.<InvitationCreateResult>fail("tenantCode 必填").code(400);
        }
        Long inviterId = extractUserId(httpRequest);
        if (inviterId == null) { return Result.<InvitationCreateResult>fail("未登录").code(401); }


        // 校验 inviter 是该租户 owner/admin
        MembershipDO ms = membershipMapper.selectByUserAndTenant(inviterId, req.getTenantCode());
        if (ms == null || ms.getMemberStatus() != 1)
            return Result.<InvitationCreateResult>fail("您不是该租户成员").code(403);
        if (!"owner".equals(ms.getRoleCode()) && !"admin".equals(ms.getRoleCode())) {
            return Result.<InvitationCreateResult>fail("仅 owner/admin 可发送邀请").code(403);
        }

        // 委托给 Service
        com.zifang.ctc.core.domain.entity.InvitationDO inv = invitationService.create(
                req.getTenantCode(), inviterId, req.getInviteeEmail(), req.getInviteePhone(),
                req.getRoleCode(), req.getMaxUses(), req.getType());

        InvitationCreateResult body = new InvitationCreateResult();
        body.setId(inv.getId());
        body.setToken(inv.getToken());
        body.setInviteCode(inv.getInviteCode());
        body.setExpireAt(inv.getExpireAt());
        body.setInviteUrl(inv.getToken() != null
                ? "https://opc.zopc.top/invite?token=" + inv.getToken()
                : null);
        log.info("InvitationController.create 成功: tenant={}, inviter={}, type={}",
                req.getTenantCode(), inviterId, req.getType());
        return Result.success(body);
    }

    /**
     * 接受邀请 (token 链接模式).
     * 重构: 改用 InvitationService.acceptByToken (业务层做状态校验 + membership 创建 + 邀请状态翻转).
     */
    @Operation(summary = "接受邀请 (链接 token)")
    @PostMapping("/accept")
    public Result<InvitationAcceptResult> accept(@RequestBody Map<String, String> body,
                                              HttpServletRequest httpRequest) {
        String token = body.get("token");
        if (token == null) { return Result.<InvitationAcceptResult>fail("token 必填").code(400); }

        Long userId = extractUserId(httpRequest);
        if (userId == null) { return Result.<InvitationAcceptResult>fail("未登录").code(401); }


        try {
            com.zifang.ctc.core.domain.entity.InvitationDO inv = invitationService.findByToken(token)
                    .orElse(null);
            if (inv == null) { return Result.<InvitationAcceptResult>fail("邀请不存在").code(404); }


            MembershipDO ms = invitationService.acceptByToken(token, userId);
            InvitationAcceptResult resp = new InvitationAcceptResult();
            resp.setTenantCode(inv.getTenantCode());
            resp.setRoleCode(inv.getRoleCode());
            resp.setMembershipId(ms.getId());
            log.info("InvitationController.accept 成功: userId={}, tenant={}", userId, inv.getTenantCode());
            return Result.success(resp);
        } catch (IllegalArgumentException e) {
            return Result.<InvitationAcceptResult>fail(e.getMessage()).code(404);
        } catch (IllegalStateException e) {
            return Result.<InvitationAcceptResult>fail(e.getMessage()).code(410);
        }
    }

    /**
     * 兑换邀请码.
     * 重构: 改用 InvitationService.redeemByCode.
     */
    @Operation(summary = "兑换邀请码")
    @PostMapping("/redeem")
    public Result<InvitationRedeemResult> redeem(@RequestBody Map<String, String> body,
                                              HttpServletRequest httpRequest) {
        String code = body.get("code");
        if (code == null) { return Result.<InvitationRedeemResult>fail("code 必填").code(400); }

        Long userId = extractUserId(httpRequest);
        if (userId == null) { return Result.<InvitationRedeemResult>fail("未登录").code(401); }


        try {
            com.zifang.ctc.core.domain.entity.InvitationDO inv = invitationService.findByInviteCode(code)
                    .orElse(null);
            if (inv == null) { return Result.<InvitationRedeemResult>fail("邀请码不存在").code(404); }


            MembershipDO ms = invitationService.redeemByCode(code, userId);
            InvitationRedeemResult resp = new InvitationRedeemResult();
            resp.setTenantCode(inv.getTenantCode());
            resp.setRoleCode(inv.getRoleCode());
            resp.setMembershipId(ms.getId());
            log.info("InvitationController.redeem 成功: userId={}, tenant={}", userId, inv.getTenantCode());
            return Result.success(resp);
        } catch (IllegalArgumentException e) {
            return Result.<InvitationRedeemResult>fail(e.getMessage()).code(404);
        } catch (IllegalStateException e) {
            return Result.<InvitationRedeemResult>fail(e.getMessage()).code(410);
        }
    }

    /**
     * 查邀请列表 (按租户).
     * 重构: 改用 InvitationService.listByTenant.
     */
    @Operation(summary = "查邀请列表")
    @GetMapping
    public Result<Object> list(@RequestParam String tenantCode, HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        if (userId == null) { return Result.fail("未登录").code(401); }

        MembershipDO ms = membershipMapper.selectByUserAndTenant(userId, tenantCode);
        if (ms == null || ms.getMemberStatus() != 1) return Result.fail("您不是该租户成员").code(403);

        if (!"owner".equals(ms.getRoleCode()) && !"admin".equals(ms.getRoleCode())) {
            return Result.fail("仅 owner/admin 可查").code(403);
        }
        return Result.success(invitationService.listByTenant(tenantCode));
    }

    /**
     * 撤销邀请.
     * 重构: 改用 InvitationService.revoke.
     */
    @Operation(summary = "撤销邀请")
    @PostMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id, @RequestParam String tenantCode,
                               HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        if (userId == null) { return Result.<Void>fail("未登录").code(401); }

        MembershipDO ms = membershipMapper.selectByUserAndTenant(userId, tenantCode);
        if (ms == null || ms.getMemberStatus() != 1) return Result.<Void>fail("您不是该租户成员").code(403);

        if (!"owner".equals(ms.getRoleCode()) && !"admin".equals(ms.getRoleCode())) {
            return Result.<Void>fail("仅 owner/admin 可撤销").code(403);
        }
        try {
            invitationService.revoke(id);
        } catch (IllegalStateException e) {
            return Result.<Void>fail(e.getMessage()).code(400);
        }
        return Result.success();
    }

    // ============== FEATURE049: 成员管理 ==============

    /**
     * 查租户成员列表 (owner/admin).
     * 重构: 改用 MembershipService.listActiveByTenant.
     */
    @Operation(summary = "查租户成员列表 (FEATURE049)")
    @GetMapping("/members")
    public Result<Object> listMembers(@RequestParam String tenantCode, HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        if (userId == null) { return Result.fail("未登录").code(401); }

        MembershipDO ms = membershipService.findByUserAndTenant(userId, tenantCode).orElse(null);
        if (ms == null || ms.getMemberStatus() == null || ms.getMemberStatus() != 1) {
            return Result.fail("您不是该租户成员").code(403);
        }
        if (!"owner".equals(ms.getRoleCode()) && !"admin".equals(ms.getRoleCode())) {
            return Result.fail("仅 owner/admin 可查").code(403);
        }
        return Result.success(membershipService.listActiveByTenant(tenantCode));
    }

    /**
     * 移除成员 (owner/admin, 不能移除 owner 也不能移除自己).
     * 重构: 改用 MembershipService.findById + MembershipService.remove.
     */
    @Operation(summary = "移除成员 (FEATURE049)")
    @PostMapping("/members/{memberId}/remove")
    public Result<Void> removeMember(@PathVariable Long memberId,
                                     @RequestParam String tenantCode,
                                     HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        if (userId == null) { return Result.<Void>fail("未登录").code(401); }

        MembershipDO callerMs = membershipService.findByUserAndTenant(userId, tenantCode).orElse(null);
        if (callerMs == null || callerMs.getMemberStatus() == null || callerMs.getMemberStatus() != 1) {
            return Result.<Void>fail("您不是该租户成员").code(403);
        }
        if (!"owner".equals(callerMs.getRoleCode()) && !"admin".equals(callerMs.getRoleCode())) {
            return Result.<Void>fail("仅 owner/admin 可移除").code(403);
        }
        MembershipDO target = membershipService.findById(memberId).orElse(null);
        if (target == null) { return Result.<Void>fail("成员不存在").code(404); }

        if (!tenantCode.equals(target.getTenantCode())) { return Result.<Void>fail("成员不属于该租户").code(400); }
        if ("owner".equals(target.getRoleCode())) { return Result.<Void>fail("不能移除 owner").code(400); }
        if (target.getUserId().equals(userId)) { return Result.<Void>fail("不能移除自己, 用退出接口").code(400); }
        if ("admin".equals(callerMs.getRoleCode()) && "admin".equals(target.getRoleCode())) {
            return Result.<Void>fail("admin 不能移除 admin").code(403);
        }
        membershipService.remove(memberId);
        log.info("InvitationController.removeMember: 移除 memberId={} from tenant={} by userId={}",
                memberId, tenantCode, userId);
        return Result.success();
    }

    /**
     * 退出当前租户 (成员自己退出, owner 不能退出 - 需先转让).
     * 重构: 改用 MembershipService.leave (Service 内部校验 owner 约束).
     */
    @Operation(summary = "退出当前租户 (FEATURE049)")
    @PostMapping("/leave")
    public Result<Void> leave(@RequestParam String tenantCode, HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        if (userId == null) { return Result.<Void>fail("未登录").code(401); }

        MembershipDO ms = membershipService.findByUserAndTenant(userId, tenantCode).orElse(null);
        if (ms == null) { return Result.<Void>fail("您不在该租户").code(404); }

        try {
            membershipService.leave(ms.getId());
        } catch (IllegalStateException e) {
            return Result.<Void>fail(e.getMessage()).code(400);
        }
        log.info("InvitationController.leave: userId={} leave tenant={}", userId, tenantCode);
        return Result.success();
    }

    // ============== helpers ==============

    private Long extractUserId(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) { return null; }
        String token = authHeader.substring(7);
        JwtUtil.VerificationResult v = jwtUtil.verifyToken(token);
        if (!v.isValid()) { return null; }
        Object userIdObj = v.getClaims().get("userId");
        if (userIdObj == null) { return null; }

        try {
            return Long.parseLong(String.valueOf(userIdObj));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // generateInviteCode 已迁移到 InvitationServiceImpl.generateUniqueInviteCode

    // ============== DTO ==============

    public static class CreateInvitationRequest {
        private String tenantCode;
        private String inviteeEmail;
        private String inviteePhone;
        private String roleCode;
        private Integer maxUses;
        private String type;  // "link" / "code"

        public String getTenantCode() {
            return tenantCode;
        }

        public void setTenantCode(String tenantCode) {
            this.tenantCode = tenantCode;
        }

        public String getInviteeEmail() {
            return inviteeEmail;
        }

        public void setInviteeEmail(String inviteeEmail) {
            this.inviteeEmail = inviteeEmail;
        }

        public String getInviteePhone() {
            return inviteePhone;
        }

        public void setInviteePhone(String inviteePhone) {
            this.inviteePhone = inviteePhone;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public Integer getMaxUses() {
            return maxUses;
        }

        public void setMaxUses(Integer maxUses) {
            this.maxUses = maxUses;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
