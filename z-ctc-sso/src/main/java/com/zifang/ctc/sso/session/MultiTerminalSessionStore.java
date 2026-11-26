package com.zifang.ctc.sso.session;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多端登录会话存储.
 * <p>
 * 设计哲学:
 * 主流 SaaS 场景下, 同一账号可能同时在 web / 移动 / 桌面 / 第三方集成多个端登录,
 * 每个端都有自己的 token. 本类用 {@code Map<userId, Map<terminalId, token>>} 维护
 * "账号 -> 端 -> token" 的关系, 提供以下业务能力:
 * <ul>
 *   <li>列出一个账号的全部在线端 (运营/审计场景)</li>
 *   <li>踢掉某个端 (用户主动"下线此设备" / 管理员强制)</li>
 *   <li>踢掉全部端 (改密 / 账号锁定)</li>
 * </ul>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>线程安全: ConcurrentHashMap 嵌套</li>
 *   <li>in-memory: 不依赖 Redis/Mysql, 单 JVM 内存持有 (多副本时各副本独立)</li>
 *   <li>可注入: 用 {@code @Component} 暴露, 业务方按需 {@code @Autowired} 即可</li>
 *   <li>不影响既有 TokenService / SsoInterceptor 行为 — 业务方登录成功/退出时显式调用</li>
 * </ul>
 */
@org.springframework.stereotype.Component
public class MultiTerminalSessionStore {

    private final Map<String, Map<String, String>> store = new ConcurrentHashMap<String, Map<String, String>>();

    /**
     * 记录 userId 在 terminalId 的会话 (token 必传).
     */
    public void register(String userId, String terminalId, String token) {
        if (userId == null || terminalId == null || token == null) {
            return;
        }
        Map<String, String> terminals = store.get(userId);
        if (terminals == null) {
            terminals = new ConcurrentHashMap<String, String>();
            Map<String, String> old = store.putIfAbsent(userId, terminals);
            if (old != null) {
                terminals = old;
            }
        }
        terminals.put(terminalId, token);
    }

    /**
     * 移除 userId 在 terminalId 的会话. 返回被移除的 token (业务方可用于 token 黑名单).
     */
    public String unregister(String userId, String terminalId) {
        if (userId == null || terminalId == null) {
            return null;
        }
        Map<String, String> terminals = store.get(userId);
        if (terminals == null) {
            return null;
        }
        return terminals.remove(terminalId);
    }

    /**
     * 踢掉 userId 的全部端. 返回被踢的所有 token.
     */
    public Set<String> unregisterAll(String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Map<String, String> terminals = store.remove(userId);
        if (terminals == null || terminals.isEmpty()) {
            return Collections.emptySet();
        }
        return new java.util.HashSet<String>(terminals.values());
    }

    /**
     * 列出 userId 当前在线的全部端 id.
     */
    public Set<String> onlineTerminals(String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Map<String, String> terminals = store.get(userId);
        if (terminals == null) {
            return Collections.emptySet();
        }
        return new java.util.HashSet<String>(terminals.keySet());
    }

    /**
     * 查询 userId 在某端的 token. 用于"踢下线后由调用方比对前端持有的 token 是否需要失效".
     */
    public String tokenOf(String userId, String terminalId) {
        if (userId == null || terminalId == null) {
            return null;
        }
        Map<String, String> terminals = store.get(userId);
        if (terminals == null) {
            return null;
        }
        return terminals.get(terminalId);
    }

    /**
     * 清空全部 (例如配置热加载 / 单元测试).
     */
    public void clear() {
        store.clear();
    }
}
