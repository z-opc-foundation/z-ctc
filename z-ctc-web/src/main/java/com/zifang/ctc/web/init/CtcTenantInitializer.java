package com.zifang.ctc.web.init;

import com.zifang.ctc.core.domain.entity.DomainDO;
import com.zifang.ctc.core.domain.entity.OrgDO;
import com.zifang.ctc.core.init.TenantInitContext;
import com.zifang.ctc.core.init.TenantInitializer;
import com.zifang.ctc.core.service.DomainService;
import com.zifang.ctc.core.service.OrgService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * z-ctc 内置租户初始化器.
 *
 * <p>新建租户后自动建立:
 * <ul>
 *   <li>默认域: domainCode="default"</li>
 *   <li>默认组织: orgCode="default", 挂在默认域下</li>
 * </ul>
 *
 * <p>order=10: 排在大多数业务模块之前, 给它们一个可以挂载数据的"根".
 * 幂等: 已存在 default 域/组织自动跳过.
 *
 * <p>异常处理: 单步异常包装成 {@link InitFailedException} 抛出,
 * Orchestrator 收到后会记 WARN 日志 + 报告为失败, 但继续执行其他 Initializer.
 * <strong>不要在内部 try/catch 吞掉, 否则报告失真 (看似成功实际数据没建).</strong>
 */
@Component
public class CtcTenantInitializer implements TenantInitializer {

    private static final Logger log = LogManager.getLogger(CtcTenantInitializer.class);

    @Autowired
    private DomainService domainService;

    @Autowired
    private OrgService orgService;

    @Override
    public String moduleName() {
        return "z-ctc";
    }

    @Override
    public int order() {
        return 10;  // 最早一批
    }

    @Override
    public void onTenantCreated(TenantInitContext ctx) {
        String tenantCode = ctx.tenantCode();
        String createdBy = ctx.createdBy() == null ? null : String.valueOf(ctx.createdBy());

        // 1. 建默认域 (default)
        Long defaultDomainId = ensureDefaultDomain(tenantCode, createdBy);

        // 2. 建默认组织 (挂在默认域下)
        ensureDefaultOrg(tenantCode, defaultDomainId, createdBy);

        log.info("[z-ctc-init] 租户 {} 初始化完成 (域+组织)", tenantCode);
    }

    /**
     * 幂等创建 default 域. 已有则返回已有 id. 失败抛出 InitFailedException 让 Orchestrator 报告.
     */
    private Long ensureDefaultDomain(String tenantCode, String createdBy) {
        try {
            List<DomainDO> existing = domainService.listByTenant(tenantCode);
            for (DomainDO d : existing) {
                if ("default".equals(d.getDomainCode())) {
                    log.debug("[z-ctc-init] 租户 {} 已有 default 域, 跳过", tenantCode);
                    return d.getId();
                }
            }
            DomainDO d = new DomainDO();
            d.setTenantCode(tenantCode);
            d.setDomainCode("default");
            d.setDomainName(tenantCode + " 默认域");
            d.setStatus(1);
            Long id = domainService.createDomain(d, createdBy);
            log.info("[z-ctc-init] 租户 {} 创建默认域 id={}", tenantCode, id);
            return id;
        } catch (Exception e) {
            // 不吞, 包装后抛出 (Orchestrator 会接住)
            throw new InitFailedException("建默认域失败: " + e.getMessage(), e);
        }
    }

    /**
     * 幂等创建 default 组织. 已有则跳过. 失败抛出 InitFailedException.
     */
    private void ensureDefaultOrg(String tenantCode, Long defaultDomainId, String createdBy) {
        if (defaultDomainId == null) {
            throw new InitFailedException("无法建组织: 默认域 id 为空 (建域失败)", null);
        }
        try {
            Optional<OrgDO> existing = orgService.findByCode(tenantCode, "default", "default");
            if (existing.isPresent()) {
                log.debug("[z-ctc-init] 租户 {} 已有 default 组织, 跳过", tenantCode);
                return;
            }
            OrgDO org = new OrgDO();
            org.setTenantCode(tenantCode);
            org.setDomainCode("default");
            org.setOrgCode("default");
            org.setOrgName(tenantCode + " 默认组织");
            org.setStatus(1);
            Long id = orgService.createOrg(org, createdBy);
            log.info("[z-ctc-init] 租户 {} 创建默认组织 id={}", tenantCode, id);
        } catch (Exception e) {
            throw new InitFailedException("建默认组织失败: " + e.getMessage(), e);
        }
    }

    /**
     * 本 Initializer 内部的统一异常. 让 Orchestrator 能区分"我自己失败" vs "Spring 容器错误".
     */
    public static class InitFailedException extends RuntimeException {
        public InitFailedException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
