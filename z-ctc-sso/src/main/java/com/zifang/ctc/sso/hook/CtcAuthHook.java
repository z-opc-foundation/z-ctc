package com.zifang.ctc.sso.hook;

import com.zifang.ctc.sso.model.UserInfo;

/**
 * CTC 认证钩子.
 *
 * <p>z-ctc-sso 定义接口, 调用方通过此钩子注入认证逻辑.
 * 支持多种实现方式:
 * <ul>
 *   <li>本地模式: 直接调用 z-ctc-core 的 service</li>
 *   <li>HTTP 模式: 调用 z-ctc-web 的 REST API</li>
 *   <li>RPC 模式: 通过 z-rpc 调用 z-ctc 服务</li>
 * </ul>
 *
 * <p>使用方式:
 * <pre>{@code
 * // HTTP 模式
 * @Bean
 * public CtcAuthHook ctcAuthHook() {
 *     return new HttpCtcAuthHook("http://z-ctc:8080");
 * }
 *
 * // RPC 模式
 * @Bean
 * public CtcAuthHook ctcAuthHook() {
 *     return new RpcCtcAuthHook(); // 使用 z-rpc @ZRpcReference
 * }
 * }</pre>
 */
public interface CtcAuthHook {

    /**
     * 验证 token 有效性.
     *
     * @param token JWT token
     * @return 有效的用户信息, 无效返回 null
     */
    UserInfo verifyToken(String token);

    /**
     * 钩子类型标识, 用于日志和调试.
     */
    default String hookType() {
        return "ctc-auth";
    }
}
