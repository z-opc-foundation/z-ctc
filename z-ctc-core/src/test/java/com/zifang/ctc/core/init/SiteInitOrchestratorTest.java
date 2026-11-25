package com.zifang.ctc.core.init;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zifang.ctc.common.spi.site.SiteInitInvokeRequest;
import com.zifang.ctc.core.domain.entity.SiteInitRegistrationDO;
import com.zifang.ctc.core.domain.entity.SiteInitTaskDO;
import com.zifang.ctc.core.service.SiteInitService;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 建站初始化编排测试 (内存桩 + 真 HTTP 协议往返, 不依赖 DB / JUnit).
 *
 * <p>验的是**标准协议本身**: 编排器用真 RemoteSiteInitClient 打一个本地 HTTP 假域,
 * 假域按标准协议暴露 /api/site-init/manifest 与 /api/site-init/invoke,
 * 断言: ①按 order 顺序逐域调用 ②每次调用落一条任务记录 ③域失败不阻断后续域
 * ④manifest 拉得到能力清单.
 */
public class SiteInitOrchestratorTest {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        try {
            run();
        } catch (Exception e) {
            // 兜底: 中途抛异常也要看到栈 + 走 report (否则 HttpServer 非守护线程吊死 JVM)
            fail++;
            System.out.println("  ✗ 测试中断: " + e);
            e.printStackTrace(System.out);
        } finally {
            report();
        }
    }

    private static void run() throws Exception {
        // ── 假域: 两个域, 一个成功一个失败; 端点按 domainCode 分路径 (协议口径) ──
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/site-init/z-fake-a/manifest", ex -> respondManifest(ex, "z-fake-a", "能力A"));
        server.createContext("/api/site-init/z-fake-b/manifest", ex -> respondManifest(ex, "z-fake-b", "能力B"));
        server.createContext("/api/site-init/z-fake-a/invoke", ex ->
                respond(ex, 200, "{\"success\":true,\"code\":0,\"message\":\"ok\",\"items\":[{\"code\":\"a.x\",\"name\":\"能力A\",\"success\":true}]}"));
        server.createContext("/api/site-init/z-fake-b/invoke", ex ->
                respond(ex, 500, "{\"success\":false,\"code\":1,\"message\":\"域内部故障\"}"));
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();

        // ── 桩注册表: 顺序 z-fake-b(10) → z-fake-a(20) ──
        StubSiteInitService svc = new StubSiteInitService();
        svc.add(reg("z-fake-b", base, 10));
        svc.add(reg("z-fake-a", base, 20));

        SiteInitOrchestrator orchestrator = new SiteInitOrchestrator(svc, new RemoteSiteInitClient(2000, 5000));

        SiteInitRunResult result = orchestrator.run("t-001", 7L, Map.of("k", "v"), "admin-1", "trace-1");

        eq("调用了两个域", 2, result.getDomains().size());
        eq("成功 1 个域", 1, result.getSuccessCount());
        eq("失败 1 个域", 1, result.getFailureCount());
        eq("失败不阻断: 第二个域仍被调用", "z-fake-a", result.getDomains().get(1).domainCode);
        eq("任务记录两条", 2, svc.tasks.size());
        eq("任务含失败域", "z-fake-b", svc.tasks.get(0).getDomainCode());
        eq("任务状态=失败", Integer.valueOf(SiteInitTaskDO.STATUS_FAILED), svc.tasks.get(0).getStatus());
        eq("任务状态=成功", Integer.valueOf(SiteInitTaskDO.STATUS_SUCCESS), svc.tasks.get(1).getStatus());
        eq("任务带幂等键", "t-001:z-fake-b:trace-1", svc.tasks.get(0).getIdempotencyKey());
        eq("任务带请求报文", true, svc.tasks.get(0).getRequestJson().contains("t-001"));

        // ── manifest 拉取 ──
        var manifest = orchestrator.manifestOf("z-fake-a");
        eq("拉到 manifest", true, manifest != null);
        eq("manifest 协议版本", "1.0", manifest.getProtocolVersion());
        eq("manifest 域编码", "z-fake-a", manifest.getDomainCode());
        eq("manifest 能力数", 1, manifest.getCapabilities().size());
        eq("manifest 能力编码", "cap-a", manifest.getCapabilities().get(0).getCode());

        // ── 单域重跑 ──
        SiteInitRunResult one = orchestrator.runDomain("t-002", null, "z-fake-a", null, "admin-1", null);
        eq("单域重跑成功 1", 1, one.getSuccessCount());

        server.stop(0);
        report();
    }

    private static SiteInitRegistrationDO reg(String domainCode, String endpoint, int order) {
        SiteInitRegistrationDO r = new SiteInitRegistrationDO();
        r.setDomainCode(domainCode);
        r.setDomainName(domainCode);
        r.setEndpoint(endpoint);
        r.setOrderNo(order);
        r.setStatus(1);
        return r;
    }

    private static void respondManifest(HttpExchange ex, String domainCode, String capabilityName) throws java.io.IOException {
        respond(ex, 200, "{\"protocolVersion\":\"1.0\",\"domainCode\":\"" + domainCode
                + "\",\"domainName\":\"假域\",\"capabilities\":[{\"code\":\"cap-a\",\"name\":\"" + capabilityName
                + "\",\"description\":\"能力\",\"order\":100}]}");
    }

    private static void respond(HttpExchange ex, int code, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void eq(String name, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass++;
        } else {
            fail++;
            System.out.println("  ✗ " + name + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void report() {
        System.out.println("SiteInitOrchestratorTest: " + pass + " passed, " + fail + " failed");
        System.out.flush();
        // HttpServer 起的是非守护线程, 不显式退出的话 JVM 不收尾
        System.exit(fail > 0 ? 1 : 0);
    }

    /** 内存桩注册表 + 任务记录 */
    static class StubSiteInitService implements SiteInitService {
        final List<SiteInitRegistrationDO> regs = new ArrayList<>();
        final List<SiteInitTaskDO> tasks = new ArrayList<>();

        void add(SiteInitRegistrationDO r) {
            regs.add(r);
        }

        @Override
        public SiteInitRegistrationDO register(String domainCode, String domainName, Integer orderNo, String endpoint,
                                              String protocolVersion, String capabilities) {
            return reg(domainCode, endpoint, orderNo == null ? 100 : orderNo);
        }

        @Override
        public List<SiteInitRegistrationDO> listAll() {
            return regs;
        }

        @Override
        public List<SiteInitRegistrationDO> listActive() {
            List<SiteInitRegistrationDO> out = new ArrayList<>();
            for (SiteInitRegistrationDO r : regs) {
                if (r.getStatus() != null && r.getStatus() == 1) {
                    out.add(r);
                }
            }
            out.sort((a, b) -> {
                int c = Integer.compare(a.getOrderNo(), b.getOrderNo());
                return c != 0 ? c : a.getDomainCode().compareTo(b.getDomainCode());
            });
            return out;
        }

        @Override
        public boolean disable(String domainCode) {
            return false;
        }

        @Override
        public SiteInitTaskDO startTask(String tenantCode, String domainCode, String capability,
                                        String idempotencyKey, String requestJson, String traceId) {
            SiteInitTaskDO t = new SiteInitTaskDO();
            t.setId((long) (tasks.size() + 1));
            t.setTenantCode(tenantCode);
            t.setDomainCode(domainCode);
            t.setCapability(capability);
            t.setIdempotencyKey(idempotencyKey);
            t.setRequestJson(requestJson);
            t.setTraceId(traceId);
            t.setStatus(SiteInitTaskDO.STATUS_RUNNING);
            tasks.add(t);
            return t;
        }

        @Override
        public void finishTask(Long taskId, int status, String responseJson, String message, long costMs) {
            for (SiteInitTaskDO t : tasks) {
                if (t.getId().equals(taskId)) {
                    t.setStatus(status);
                    t.setResponseJson(responseJson);
                    t.setMessage(message);
                    t.setCostMs(costMs);
                }
            }
        }

        @Override
        public SiteInitTaskDO findTask(Long id) {
            return null;
        }

        @Override
        public SiteInitTaskDO findByIdempotencyKey(String idempotencyKey) {
            return null;
        }

        @Override
        public List<SiteInitTaskDO> pageTasks(String tenantCode, String domainCode, int pageNum, int pageSize, long[] totalOut) {
            Map<String, Object> ignored = new LinkedHashMap<>();
            return tasks;
        }
    }
}
