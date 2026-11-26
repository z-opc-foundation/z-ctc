package com.zifang.ctc.sso.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SSO 闸门两件事的回归测试（卡 TASK-20261022-001 第 1 项 b / 第 2 项）：
 *
 * <ol>
 *   <li><b>客户端来源 IP 取真实来源</b>：{@code SsoInterceptor.resolveClientIp}
 *       —— 口径 = 服务端对端地址 + {@code X-Forwarded-For} 链，<b>以最后一跳可信地址为准</b>。
 *       此前登录日志只从 {@code X-Client-Ip} 头取值 ⇒ 没有组件注入该头时**恒为 NULL**
 *       （实测 1863 行 100% 为 NULL）⇒ 字段存在 ≠ 能按 IP 归因。</li>
 *   <li><b>闸门拒绝必须留痕</b>：拒绝日志里要有 <b>URL + 客户端 IP + 拒绝原因</b> 三样，
 *       且**不能把 token 值写进日志**（源码级守卫）。</li>
 * </ol>
 */
class SsoInterceptorClientIpTest {

    /** 用动态代理造一个最小 HttpServletRequest（z-ctc-sso 没有 spring-test 依赖）。 */
    private static HttpServletRequest fakeRequest(String remoteAddr, String xff) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getRemoteAddr":
                            return remoteAddr;
                        case "getHeader":
                            if (args != null && args.length > 0 && args[0] != null
                                    && "X-Forwarded-For".equalsIgnoreCase((String) args[0])) {
                                return xff;
                            }
                            return null;
                        case "getRequestURI":
                            return "/api/ctc/authn/whoami";
                        case "getMethod":
                            return "GET";
                        case "toString":
                            return "FakeHttpServletRequest";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == (args == null ? null : args[0]);
                        default:
                            return null;
                    }
                });
    }

    @Test
    @DisplayName("没有 XFF 时取服务端对端地址（网关/容器看到的真实连接方）")
    void noXffUsesPeerAddress() {
        assertEquals("10.42.0.7",
                SsoInterceptor.resolveClientIp(fakeRequest("10.42.0.7", null)));
    }

    @Test
    @DisplayName("XFF 带公网客户端 + 私网对端 ⇒ 取 XFF 里的真实客户端")
    void xffPublicClientWins() {
        assertEquals("203.0.113.9",
                SsoInterceptor.resolveClientIp(fakeRequest("10.42.0.7", "203.0.113.9")));
    }

    @Test
    @DisplayName("XFF 前置项被伪造 ⇒ 忽略靠左的伪造项，取链上最后一个可信地址")
    void forgedLeftmostIsIgnored() {
        // 边缘网关把自己看到的地址追加到链尾 ⇒ 链尾那项才是可信的
        assertEquals("198.51.100.23",
                SsoInterceptor.resolveClientIp(
                        fakeRequest("10.42.0.7", "6.6.6.6, 198.51.100.23")));
    }

    @Test
    @DisplayName("整条链都是私网（客户端在内网）⇒ 退回 XFF 最左一项")
    void allPrivateFallsBackToLeftmost() {
        assertEquals("10.1.2.3",
                SsoInterceptor.resolveClientIp(fakeRequest("10.42.0.7", "10.1.2.3, 10.42.0.7")));
    }

    @Test
    @DisplayName("对端与 XFF 都取不到 ⇒ 返回 null（不写脏值）")
    void nothingAvailableReturnsNull() {
        assertNull(SsoInterceptor.resolveClientIp(fakeRequest(null, null)));
    }

    @Test
    @DisplayName("闸门拒绝留痕：源码里必须有 URL + 客户端 IP + 拒绝原因，且不得打印 token 值")
    void rejectionLoggingIsPresent() throws Exception {
        Path src = Paths.get("src/main/java/com/zifang/ctc/sso/config/SsoInterceptor.java");
        assertTrue(Files.exists(src), "找不到 SsoInterceptor 源码: " + src.toAbsolutePath());
        String body = new String(Files.readAllBytes(src), StandardCharsets.UTF_8);

        assertTrue(body.contains("SSO-GATE"), "拒绝日志缺少可检索的标记 [SSO-GATE]");
        assertTrue(body.contains("url="), "拒绝日志缺少 URL");
        assertTrue(body.contains("clientIp="), "拒绝日志缺少客户端 IP");
        assertTrue(body.contains("reason="), "拒绝日志缺少拒绝原因");
        // 不得把凭证内容写进日志：拒绝分支里不允许出现 token 变量本身
        assertTrue(!body.contains("log.warn(\"[SSO-GATE] 拒绝: url={}, method={}, clientIp={}, reason={}, ua={}, token={}"),
                "拒绝日志把 token 值写进了日志");
    }
}
