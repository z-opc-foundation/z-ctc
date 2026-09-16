package com.zifang.ctc.sso.hook;

/**
 * CTC 授权钩子.
 *
 * <p>z-ctc-sso 定义接口, 调用方通过此钩子注入权限校验逻辑.
 * 支持多种实现方式 (HTTP / RPC / 本地).
 *
 * <p>使用方式:
 * <pre>{@code
 * // SsoInterceptor 中使用
 * if (ctcAuthorizeHook.hasPermission(userId, "user:create")) {
 *     // 放行
 * }
 * }</pre>
 */
public interface CtcAuthorizeHook {

    /**
     * 校验用户是否拥有指定权限.
     *
     * @param userId     用户 ID
     * @param permission 权限标识 (如 "user:create", "order:read")
     * @return true 有权限, false 无权限
     */
    boolean hasPermission(String userId, String permission);

    /**
     * 校验用户是否拥有指定角色.
     *
     * @param userId 用户 ID
     * @param role   角色标识 (如 "admin", "editor")
     * @return true 有角色, false 无角色
     */
    boolean hasRole(String userId, String role);

    /**
     * 钩子类型标识.
     */
    default String hookType() {
        return "ctc-authorize";
    }
}
