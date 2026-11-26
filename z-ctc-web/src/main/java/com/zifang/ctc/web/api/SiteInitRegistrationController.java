package com.zifang.ctc.web.api;

import com.zifang.ctc.common.spi.site.SiteInitManifest;
import com.zifang.ctc.common.spi.site.SiteInitRegistration;
import com.zifang.ctc.core.domain.entity.SiteInitRegistrationDO;
import com.zifang.ctc.core.service.SiteInitService;
import com.zifang.ctc.web.api.annotation.NoLogin;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.ResultCode;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 建站初始化 · 域注册端点 (标准协议 ctc 侧).
 *
 * <p>各域把自己的初始化能力登记到 4A 中心: ctc 感知注册表并按标准协议编排.
 *
 * <p>端点:
 * <ul>
 *   <li>POST /api/ctc/site-init/registrations         - 注册 (幂等, 同 domainCode 覆盖; 机器对机器)</li>
 *   <li>GET  /api/ctc/site-init/registrations         - 注册表列表</li>
 *   <li>POST /api/ctc/site-init/registrations/disable - 停用某域注册</li>
 * </ul>
 */
@RestController("siteInitRegistrationController")
@RequestMapping("/api/ctc/site-init/registrations")
public class SiteInitRegistrationController {

    private final SiteInitService siteInitService;

    /** 服务间登记令牌: 未配置时注册端点一律拒绝 (fail closed) */
    @Value("${z.ctc.site-init.register-token:}")
    private String registerToken;

    public SiteInitRegistrationController(SiteInitService siteInitService) {
        this.siteInitService = siteInitService;
    }

    /**
     * 域注册 (机器对机器): 免登录, 但必须带登记令牌.
     */
    @PostMapping
    @NoLogin
    @BizApi(summary = "注册建站初始化域 (机器对机器, 需登记令牌)", description = "注册建站初始化域 (机器对机器, 需登记令牌).", tags = { "z-ctc" })
    public Result<SiteInitRegistrationDO> register(
            @RequestBody @BizParam(name = "registration", type = BizField.FieldType.STRING, description = "注册报文", position = BizParam.Position.BODY) SiteInitRegistration registration,
            @RequestHeader(value = "X-Site-Init-Token", required = false)
            @BizParam(name = "X-Site-Init-Token", type = BizField.FieldType.STRING, description = "服务间登记令牌", position = BizParam.Position.HEADER) String token) {
        if (registerToken == null || registerToken.isEmpty()) {
            return Result.<SiteInitRegistrationDO>fail("未配置登记令牌, 拒绝注册").code(ResultCode.REQ_REJECT.getCode());
        }
        if (!registerToken.equals(token)) {
            return Result.<SiteInitRegistrationDO>fail("登记令牌无效").code(ResultCode.REQ_REJECT.getCode());
        }
        if (registration == null || registration.getDomainCode() == null || registration.getDomainCode().isEmpty()) {
            return Result.<SiteInitRegistrationDO>fail("domainCode 必填").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        if (registration.getEndpoint() == null || registration.getEndpoint().isEmpty()) {
            return Result.<SiteInitRegistrationDO>fail("endpoint 必填").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        String version = registration.getProtocolVersion();
        if (version == null || version.isEmpty()) {
            version = SiteInitManifest.PROTOCOL_VERSION;
        }
        if (!SiteInitManifest.PROTOCOL_VERSION.equals(version)) {
            return Result.<SiteInitRegistrationDO>fail("协议版本不匹配: " + version).code(ResultCode.REQ_REJECT.getCode());
        }
        SiteInitRegistrationDO saved = siteInitService.register(
                registration.getDomainCode(),
                registration.getDomainName() == null ? registration.getDomainCode() : registration.getDomainName(),
                registration.getOrder(),
                registration.getEndpoint(),
                version,
                String.join(",", registration.getCapabilities()));
        return Result.success(saved);
    }

    @GetMapping
    @BizApi(summary = "建站初始化域注册表", description = "建站初始化域注册表.", tags = { "z-ctc" })
    public Result<List<SiteInitRegistrationDO>> list() {
        return Result.success(new ArrayList<>(siteInitService.listAll()));
    }

    @PostMapping("/disable")
    @BizApi(summary = "停用某域的建站初始化注册", description = "停用某域的建站初始化注册.", tags = { "z-ctc" })
    public Result<Void> disable(@RequestBody @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码", position = BizParam.Position.BODY) String domainCode) {
        if (domainCode == null || domainCode.isEmpty()) {
            return Result.<Void>fail("domainCode 必填").code(ResultCode.PARAM_VALID_ERROR.getCode());
        }
        boolean ok = siteInitService.disable(domainCode);
        return ok ? Result.<Void>success() : Result.<Void>fail("域未注册").code(ResultCode.NOT_FOUND.getCode());
    }
}
