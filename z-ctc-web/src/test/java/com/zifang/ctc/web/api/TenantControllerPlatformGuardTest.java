package com.zifang.ctc.web.api;

import com.zifang.ctc.common.vo.PageResponseVO;
import com.zifang.ctc.core.domain.entity.MembershipDO;
import com.zifang.ctc.core.domain.entity.TenantDO;
import com.zifang.ctc.core.init.TenantInitOrchestrator;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.core.service.PlatformAdminService;
import com.zifang.ctc.core.service.TenantService;
import com.zifang.ctc.core.tenant.TenantContext;
import com.zifang.ctc.core.vo.TenantVO;
import com.zifang.util.core.meta.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TenantController 平台级租户管理面板守卫测试.
 * <p>
 * 钉住口径:
 * <ol>
 *   <li>平台级写操作 (status/update/delete) — 非平台管理员一律 403, 不触达 Service;</li>
 *   <li>全量视图 (list/page) — 平台管理员看全部租户, 普通用户只看自己 membership;</li>
 *   <li>详情 findByCode — 非平台管理员且非该租户成员 ⇒ 404.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class TenantControllerPlatformGuardTest {

    @Mock
    private TenantService tenantService;
    @Mock
    private TenantInitOrchestrator tenantInitOrchestrator;
    @Mock
    private PlatformAdminService platformAdminService;
    @Mock
    private MembershipService membershipService;

    private TenantController controller;

    @BeforeEach
    void setUp() {
        controller = new TenantController(tenantService, tenantInitOrchestrator, platformAdminService);
        // membershipService 是 @Autowired 字段注入, @InjectMocks 不管它 — 手动注入
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "membershipService", membershipService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void login(Long userId, String tenantCode, String roleCode) {
        TenantContext.set(TenantContext.TenantInfo.of(userId, tenantCode, null, roleCode));
    }

    private TenantDO tenant(String code) {
        TenantDO t = new TenantDO();
        t.setTenantCode(code);
        t.setTenantName("租户-" + code);
        t.setStatus(1);
        return t;
    }

    // ===== 写操作门槛 =====

    @Test
    @DisplayName("非平台管理员调 status ⇒ 403 且不触达 Service")
    void statusDeniedForNonPlatformAdmin() {
        login(1L, "tenant_a", "admin"); // 租户内 admin ≠ 平台管理员
        when(platformAdminService.isPlatformAdmin(1L)).thenReturn(false);

        Result<Void> r = controller.updateStatus("tenant_a", 0);
        assertEquals(403, r.getCode());
        verify(tenantService, never()).updateStatus(anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("平台管理员调 status ⇒ 放行")
    void statusAllowedForPlatformAdmin() {
        login(9L, null, null);
        when(platformAdminService.isPlatformAdmin(9L)).thenReturn(true);
        when(tenantService.updateStatus("tenant_a", 0)).thenReturn(true);

        Result<Void> r = controller.updateStatus("tenant_a", 0);
        assertTrue(r.isSuccess() || r.getCode() == 200);
    }

    @Test
    @DisplayName("非平台管理员调 update/delete ⇒ 403")
    void updateDeleteDenied() {
        login(1L, "tenant_a", "owner"); // 租户内 owner 也不行
        when(platformAdminService.isPlatformAdmin(1L)).thenReturn(false);

        assertEquals(403, controller.update("tenant_a", new TenantDO()).getCode());
        assertEquals(403, controller.delete("tenant_a").getCode());
    }

    // ===== 全量视图 =====

    @Test
    @DisplayName("平台管理员 list ⇒ 全部租户 (含自己没 membership 的)")
    void listAllForPlatformAdmin() {
        login(9L, null, null);
        when(platformAdminService.isPlatformAdmin(9L)).thenReturn(true);
        when(tenantService.listAll())
                .thenReturn(List.of(tenant("t1"), tenant("t2"), tenant("t3")));

        Result<List<TenantVO>> r = controller.listAll();
        assertEquals(3, r.getData().size());
        verify(membershipService, never()).listActiveByUserId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("普通用户 list ⇒ 仅自己所属租户")
    void listFilteredForNormalUser() {
        login(1L, null, null);
        when(platformAdminService.isPlatformAdmin(1L)).thenReturn(false);
        MembershipDO ms = new MembershipDO();
        ms.setUserId(1L);
        ms.setTenantCode("t1");
        ms.setMemberStatus(1);
        when(membershipService.listActiveByUserId(1L)).thenReturn(List.of(ms));
        when(tenantService.findByTenantCode("t1")).thenReturn(Optional.of(tenant("t1")));

        Result<List<TenantVO>> r = controller.listAll();
        assertEquals(1, r.getData().size());
        assertEquals("t1", r.getData().get(0).getTenantCode());
    }

    @Test
    @DisplayName("平台管理员 page ⇒ 全量分页")
    void pageAllForPlatformAdmin() {
        login(9L, null, null);
        when(platformAdminService.isPlatformAdmin(9L)).thenReturn(true);
        when(tenantService.listAll()).thenReturn(List.of(tenant("t1"), tenant("t2")));

        Result<PageResponseVO<TenantVO>> r = controller.page(null, 1, 20);
        assertEquals(2, r.getData().getTotal());
    }

    // ===== 详情 =====

    @Test
    @DisplayName("非平台管理员且非成员查详情 ⇒ 404")
    void detailDeniedForOutsider() {
        login(1L, "tenant_a", "member");
        when(platformAdminService.isPlatformAdmin(1L)).thenReturn(false);
        when(membershipService.findByUserAndTenant(1L, "secret_tenant")).thenReturn(Optional.empty());

        Result<TenantVO> r = controller.findByCode("secret_tenant");
        assertEquals(404, r.getCode());
        verify(tenantService, never()).findByTenantCode(anyString());
    }

    @Test
    @DisplayName("租户成员查自己租户详情 ⇒ 放行")
    void detailAllowedForMember() {
        login(1L, "tenant_a", "member");
        when(platformAdminService.isPlatformAdmin(1L)).thenReturn(false);
        MembershipDO ms = new MembershipDO();
        ms.setUserId(1L);
        ms.setTenantCode("tenant_a");
        ms.setMemberStatus(1);
        when(membershipService.findByUserAndTenant(1L, "tenant_a")).thenReturn(Optional.of(ms));
        when(tenantService.findByTenantCode("tenant_a")).thenReturn(Optional.of(tenant("tenant_a")));

        Result<TenantVO> r = controller.findByCode("tenant_a");
        assertEquals("tenant_a", r.getData().getTenantCode());
    }

    @Test
    @DisplayName("平台管理员查任意租户详情 ⇒ 放行 (无需 membership)")
    void detailAllowedForPlatformAdmin() {
        login(9L, null, null);
        when(platformAdminService.isPlatformAdmin(9L)).thenReturn(true);
        when(tenantService.findByTenantCode("any_tenant")).thenReturn(Optional.of(tenant("any_tenant")));

        Result<TenantVO> r = controller.findByCode("any_tenant");
        assertEquals("any_tenant", r.getData().getTenantCode());
    }

    // ===== create 保持开放 (OrphanGuard 白名单依赖) =====

    @Test
    @DisplayName("游离态用户 create ⇒ 仍放行 (自助开通, PLATFORM_ADMIN 判定不阻断)")
    void createStillOpenForOrphan() {
        login(7L, null, null);
        lenient().when(platformAdminService.isPlatformAdmin(7L)).thenReturn(false);
        when(tenantService.createTenant(any(TenantDO.class), eq("7"))).thenReturn(100L);

        Result<Long> r = controller.create(tenant("new_t"), "7");
        assertEquals(201, r.getCode());
    }
}
