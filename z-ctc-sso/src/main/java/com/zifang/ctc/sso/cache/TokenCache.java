package com.zifang.ctc.sso.cache;

import com.zifang.ctc.sso.model.UserInfo;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 验证结果本地缓存.
 * <p>
 * 设计哲学:
 * z-ctc-sso 在请求拦截链路 (SsoInterceptor) 中每次都调用
 * {@link com.zifang.ctc.sso.config.TokenService#verifyToken(String)}.
 * 当 {@code TokenService} 走远端校验 (RemoteTokenService) 时, 每次请求都要 HTTP 一次,
 * 性能/延迟都不友好. 本地缓存把"同一个 token 在 60s 内"复用为同一份 UserInfo,
 * 避免重复远端调用.
 * <p>
 * 关键设计:
 * <ul>
 *   <li>线程安全: ConcurrentHashMap</li>
 *   <li>过期: 写入 60s 后自动失效 (按写入时间戳)</li>
 *   <li>容量: 软上限 4096 条 (满时按 LRU 思路淘汰过期最久的)</li>
 *   <li>无负面缓存: 校验失败 (null) 不入缓存, 避免攻击者用错误 token 缓存污染</li>
 *   <li>无远端依赖: 仅在进程内, 不引入 Redis (避免新依赖; 多副本时各副本独立缓存, 互不影响)</li>
 * </ul>
 * <p>
 * 与 z-ctc-sso 既有 API 的兼容:
 * <ul>
 *   <li>不修改 TokenService 接口 (verifyToken 签名不变)</li>
 *   <li>不修改 SsoInterceptor (SsoInterceptor 仍按接口调用 TokenService)</li>
 *   <li>本类作为可选工具, 业务方按需 {@code @Autowired} 使用 (例如 RemoteTokenService 内部可包一层)</li>
 * </ul>
 */
public class TokenCache {

    private static final long DEFAULT_TTL_MILLIS = 60_000L;
    private static final int DEFAULT_MAX_SIZE = 4096;

    private final long ttlMillis;
    private final int maxSize;

    private final Map<String, Entry> cache = new ConcurrentHashMap<String, Entry>();

    public TokenCache() {
        this(DEFAULT_TTL_MILLIS, DEFAULT_MAX_SIZE);
    }

    public TokenCache(long ttlMillis, int maxSize) {
        this.ttlMillis = ttlMillis;
        this.maxSize = maxSize;
    }

    /**
     * 取缓存的 UserInfo. 命中且未过期返回, 否则返回 null.
     */
    public UserInfo get(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Entry e = cache.get(token);
        if (e == null) {
            return null;
        }
        if (isExpired(e)) {
            cache.remove(token, e);
            return null;
        }
        return e.userInfo;
    }

    /**
     * 写入缓存. 仅非空 UserInfo 入缓存 (避免负面缓存).
     */
    public void put(String token, UserInfo userInfo) {
        if (token == null || token.isEmpty() || userInfo == null) {
            return;
        }
        if (cache.size() >= maxSize) {
            evictExpired();
            if (cache.size() >= maxSize) {
                evictOldest();
            }
        }
        cache.put(token, new Entry(userInfo, System.currentTimeMillis()));
    }

    /**
     * 主动失效某个 token (例如 refresh / logout 时调用).
     */
    public void invalidate(String token) {
        if (token == null) {
            return;
        }
        cache.remove(token);
    }

    /**
     * 清空全部 (例如配置热加载时调用).
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 当前缓存条数 (含可能过期的, 仅用于监控).
     */
    public int size() {
        return cache.size();
    }

    private boolean isExpired(Entry e) {
        return System.currentTimeMillis() - e.createdAt > ttlMillis;
    }

    private void evictExpired() {
        Iterator<Map.Entry<String, Entry>> it = cache.entrySet().iterator();
        long now = System.currentTimeMillis();
        while (it.hasNext()) {
            Map.Entry<String, Entry> me = it.next();
            if (now - me.getValue().createdAt > ttlMillis) {
                it.remove();
            }
        }
    }

    private void evictOldest() {
        // 简化: 找到 createdAt 最小的一条淘汰
        String oldestKey = null;
        long oldestTs = Long.MAX_VALUE;
        for (Map.Entry<String, Entry> me : cache.entrySet()) {
            if (me.getValue().createdAt < oldestTs) {
                oldestTs = me.getValue().createdAt;
                oldestKey = me.getKey();
            }
        }
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    private static final class Entry {
        final UserInfo userInfo;
        final long createdAt;

        Entry(UserInfo userInfo, long createdAt) {
            this.userInfo = userInfo;
            this.createdAt = createdAt;
        }
    }
}
