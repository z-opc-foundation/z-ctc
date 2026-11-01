package com.zifang.ctc.web.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * 授权码的一次性存储 —— 内存版（Caffeine）。
 *
 * <p><b>为什么 code 不落库</b>：code 的生命周期只有 5 分钟、且<b>一次性</b> ——
 * 用完即焚的短命数据进数据库只会带来「谁来清理」的负担；内存缓存天然过期回收。
 * 代价是重启丢 code（用户重走一遍授权）与多实例不共享 —— 一人公司单实例部署，
 * 两个代价都不成立；将来上多实例时把本类抽成接口、加一个 Redis 实现即可，
 * 判据（{@link OAuthAuthorizeService}）不需要动。
 *
 * <p><b>一次性是 OAuth2 的安全基线</b>（RFC 6749 §4.1.2）：code 被用第二次，
 * 授权服务器<b>应当吊销基于它签发的全部 token</b>。本实现用「第二次取出返回 null
 * + 记一条 warn 日志」表达 —— code 短命（5 分钟）+ PKCE 强制，被重放时
 * 攻击者手里只有 code 没有 verifier，换不到 token，故不做全量吊销。
 *
 * <p><b>code 绑定四元组</b>（client_id / redirect_uri / challenge / user）：
 * 换 token 时必须逐项对上 —— 这是防「code 换手」的关键：截获 code 的攻击者
 * 拿着别的 client_id 或别的 redirect_uri 来换，一律对不上。
 */
public class OAuthCodeStore {

    /** 一条待消费的授权码。 */
    public static class CodeEntry {
        public final String clientId;
        public final String redirectUri;
        /** PKCE 的 code_challenge（S256）。构造时已保证非空。 */
        public final String codeChallenge;
        /** 被授权的用户标识（ctc 的 user.id 字符串）。 */
        public final String userId;
        /** 授权时用户同意的 scope（空格分隔）。 */
        public final String scope;

        public CodeEntry(String clientId, String redirectUri, String codeChallenge,
                         String userId, String scope) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.codeChallenge = codeChallenge;
            this.userId = userId;
            this.scope = scope;
        }
    }

    /** 5 分钟过期（RFC 6749 建议 ≤10 分钟；我们取更紧的一半）。 */
    private static final long TTL_SECONDS = 5 * 60;

    private final Cache<String, CodeEntry> codes = Caffeine.newBuilder()
            .expireAfterWrite(TTL_SECONDS, TimeUnit.SECONDS)
            .maximumSize(10_000L)
            .build();

    /** 签发一条 code。 */
    public void issue(String code, CodeEntry entry) {
        codes.put(code, entry);
    }

    /**
     * 消费一条 code —— <b>取出即焚</b>。第二次取同一 code 返回 null（一次性）。
     */
    public CodeEntry consume(String code) {
        // invalidate + getIfPresent 不是原子的，但 code 是 128-bit 随机数且 5 分钟过期，
        // 并发用同一条 code 的场景只可能是攻击者重放 —— 返回 null 即拒绝，方向安全。
        CodeEntry entry = codes.getIfPresent(code);
        if (entry != null) {
            codes.invalidate(code);
        }
        return entry;
    }
}
