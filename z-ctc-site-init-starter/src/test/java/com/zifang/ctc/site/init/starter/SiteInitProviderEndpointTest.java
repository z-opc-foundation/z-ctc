package com.zifang.ctc.site.init.starter;

import com.zifang.ctc.common.spi.site.SiteInitCapability;
import com.zifang.ctc.common.spi.site.SiteInitInvokeRequest;
import com.zifang.ctc.common.spi.site.SiteInitInvokeResult;
import com.zifang.ctc.common.spi.site.SiteInitManifest;
import com.zifang.ctc.common.spi.site.SiteInitProvider;

import java.util.List;

/**
 * 聚合端点分发测试 (纯内存, 不依赖 DB / JUnit / Spring 容器).
 *
 * <p>验的是多域宿主的关键语义: 一个 JVM 挂多个 SiteInitProvider 时,
 * 端点按 domainCode 各自分发、未知域不串门 (manifest 返回 null / invoke 返回失败)。
 */
public class SiteInitProviderEndpointTest {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            fail++;
            System.out.println("  ✗ 测试中断: " + e);
            e.printStackTrace(System.out);
        } finally {
            System.out.println("SiteInitProviderEndpointTest: " + pass + " passed, " + fail + " failed");
            System.out.flush();
            System.exit(fail > 0 ? 1 : 0);
        }
    }

    private static void run() {
        SiteInitProvider a = fake("z-a", "域A", cap("a.x", "能力A"));
        SiteInitProvider b = fake("z-b", "域B", cap("b.x", "能力B"));
        SiteInitProviderEndpoint endpoint = new SiteInitProviderEndpoint(List.of(a, b));

        SiteInitManifest ma = endpoint.manifest("z-a");
        eq("A 清单存在", true, ma != null);
        eq("A 清单补域编码", "z-a", ma.getDomainCode());
        eq("A 清单补域名", "域A", ma.getDomainName());
        eq("A 清单补协议版本", "1.0", ma.getProtocolVersion());
        eq("A 清单能力", "a.x", ma.getCapabilities().get(0).getCode());

        SiteInitManifest mb = endpoint.manifest("z-b");
        eq("B 清单域编码", "z-b", mb.getDomainCode());
        eq("B 清单能力", "b.x", mb.getCapabilities().get(0).getCode());

        eq("未知域清单为 null", null, endpoint.manifest("z-nope"));

        SiteInitInvokeResult ra = endpoint.invoke("z-a", new SiteInitInvokeRequest());
        eq("A 调用成功", true, ra.isSuccess());
        SiteInitInvokeResult rn = endpoint.invoke("z-nope", new SiteInitInvokeRequest());
        eq("未知域调用失败", true, !rn.isSuccess());
        eq("失败带域编码提示", true, rn.getMessage().contains("z-nope"));

        SiteInitProviderEndpoint empty = new SiteInitProviderEndpoint(List.of());
        eq("空宿主清单为 null", null, empty.manifest("z-a"));
        eq("空宿主调用失败", true, !empty.invoke("z-a", new SiteInitInvokeRequest()).isSuccess());
    }

    private static SiteInitCapability cap(String code, String name) {
        SiteInitCapability c = new SiteInitCapability();
        c.setCode(code);
        c.setName(name);
        c.setOrder(100);
        return c;
    }

    private static SiteInitProvider fake(String domainCode, String domainName, SiteInitCapability capability) {
        return new SiteInitProvider() {
            @Override
            public String domainCode() {
                return domainCode;
            }

            @Override
            public String domainName() {
                return domainName;
            }

            @Override
            public SiteInitManifest manifest() {
                SiteInitManifest m = new SiteInitManifest();
                m.getCapabilities().add(capability);
                return m;
            }

            @Override
            public SiteInitInvokeResult invoke(SiteInitInvokeRequest request) {
                return SiteInitInvokeResult.ok();
            }
        };
    }

    private static void eq(String name, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass++;
        } else {
            fail++;
            System.out.println("  ✗ " + name + ": expected=" + expected + " actual=" + actual);
        }
    }
}
