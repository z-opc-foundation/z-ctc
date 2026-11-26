package com.zifang.ctc.sso.config;

import com.zifang.ctc.sso.model.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合 TokenService — 按顺序尝试多个 {@link TokenService} 实现, 第一个返回非 null 即视为成功.
 *
 * <p>用途: 解决旧 SSO (UUID 格式 token) 与新 SSO (JWT 格式 token) 共存场景下的 401 问题.
 *
 * <p>典型链路:
 * <ol>
 *   <li>LocalTokenService: 直接 JwtUtil 验证 (HS256, 同一个 JVM)</li>
 *   <li>RemoteTokenService: HTTP 调用 auth-server-url 验证 (兼容 idea-bfcfe0a7 等旧 SSO)</li>
 * </ol>
 *
 * <p>新增链路时只需把 TokenService 添加到 {@code delegates} 列表, 无需改 SsoInterceptor.
 *
 * @author zifang
 * @date 2026/08/18
 */
public class CompositeTokenService implements TokenService {

    private static final Logger log = LogManager.getLogger(CompositeTokenService.class);

    private final List<TokenService> delegates;

    public CompositeTokenService(List<TokenService> delegates) {
        this.delegates = delegates == null ? new ArrayList<>() : new ArrayList<>(delegates);
        log.info("[CompositeTokenService] 初始化, 链表数量={}", this.delegates.size());
    }

    public void addDelegate(TokenService delegate) {
        if (delegate != null) {
            this.delegates.add(delegate);
        }
    }

    @Override
    public UserInfo verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        for (TokenService svc : delegates) {
            try {
                UserInfo info = svc.verifyToken(token);
                if (info != null) {
                    return info;
                }
            } catch (Throwable e) {
                // 单个 delegate 失败不中断整链
                log.warn("[CompositeTokenService] {} 验证异常: {}", svc.getClass().getSimpleName(), e.getMessage());
            }
        }
        return null;
    }
}
