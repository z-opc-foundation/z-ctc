package com.zifang.ctc.web.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务 (注册 / 手机验证码登录 / 找回密码).
 * <p>
 * 内存缓存 (Caffeine) 存验证码, 单机有效 — z-opc 当前 all-in-one 部署够用;
 * 多实例部署时再换 z-cache 分布式实现.
 * <p>
 * 规则:
 * <ul>
 *   <li>6 位数字码, 5 分钟有效</li>
 *   <li>同一 receiver+scene 60s 发送冷却</li>
 *   <li>同一验证码错误 5 次后作废 (防爆破)</li>
 *   <li>验证成功一次性消费</li>
 * </ul>
 */
@Service
public class VerifyCodeService {

    private static final Logger log = LogManager.getLogger(VerifyCodeService.class);

    public static final String CHANNEL_PHONE = "PHONE";
    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String SCENE_LOGIN = "LOGIN";
    public static final String SCENE_REGISTER = "REGISTER";
    public static final String SCENE_RESET = "RESET";

    private static final long CODE_EXPIRE_SECONDS = 5 * 60L;
    private static final long SEND_COOLDOWN_SECONDS = 60L;
    private static final int MAX_VERIFY_FAILURES = 5;
    private static final int CODE_BOUND = 1_000_000;

    /**
     * 验证码随机源。
     * <p>
     * 早前是 {@code String.format("%06d", (int)(Math.random() * 1_000_000))}。
     * {@code Math.random()} 走 {@link java.util.Random} —— 48 位线性同余发生器，
     * 状态空间 2^48。本机实测：连续观察 3 次 31 位输出即可精确恢复内部状态，
     * 随后 10 次输出预测值与真实值逐个完全一致。
     * 6 位码虽然只保留 53 位 double 的约 19.93 位，但 3 个验证码携带的信息量就已超过
     * 48 位状态空间 —— 也就是说攻击者的搜索空间是 PRNG 状态，不是那 10^6 个号码。
     * 用 {@code nextInt(bound)} 而非 {@code (int)(nextDouble()*bound)}，顺带避开取模偏置。
     */
    private SecureRandom random = new SecureRandom();

    /** 包可见：测试注入可预测随机源，验证生成路径确实走这个字段。 */
    void setRandomForTest(SecureRandom random) {
        this.random = random;
    }

    /** 生成 6 位数字验证码。 */
    String newCode() {
        return String.format("%06d", random.nextInt(CODE_BOUND));
    }

    /** key: scene:channel:receiver */
    private final Cache<String, CodeEntry> codeStore = Caffeine.newBuilder()
            .expireAfterWrite(CODE_EXPIRE_SECONDS, TimeUnit.SECONDS)
            .maximumSize(100_000L)
            .build();

    @Autowired(required = false)
    private List<CodeChannelSender> channelSenders = Collections.emptyList();

    /**
     * 发送验证码结果.
     */
    public static class SendOutcome {
        public final boolean sent;
        public final long cooldownSeconds;   // 冷却剩余秒数 (未到冷却期时 > 0)
        public final String message;         // 失败原因 (发送失败时)

        SendOutcome(boolean sent, long cooldownSeconds, String message) {
            this.sent = sent;
            this.cooldownSeconds = cooldownSeconds;
            this.message = message;
        }
    }

    private static class CodeEntry {
        final String code;
        final long sentAt;
        int failCount;

        CodeEntry(String code, long sentAt) {
            this.code = code;
            this.sentAt = sentAt;
        }
    }

    /**
     * 生成并发送验证码 (含 60s 冷却). 通道无 SPI 实现时打日志 (Mock 模式).
     *
     * @param receiver 接收方 (手机号 / 邮箱)
     * @param channel  PHONE / EMAIL
     * @param scene    LOGIN / REGISTER / RESET
     */
    public SendOutcome sendCode(String receiver, String channel, String scene) {
        String key = cacheKey(scene, channel, receiver);
        long now = Instant.now().getEpochSecond();
        CodeEntry existing = codeStore.getIfPresent(key);
        if (existing != null && now - existing.sentAt < SEND_COOLDOWN_SECONDS) {
            long remain = SEND_COOLDOWN_SECONDS - (now - existing.sentAt);
            return new SendOutcome(false, remain, "发送太频繁, 请 " + remain + " 秒后再试");
        }

        String code = newCode();

        // 找通道 SPI; 没有则日志 Mock (all-in-one dev 场景: 手机码不可能真发, 日志兜底)
        CodeChannelSender sender = channelSenders.stream()
                .filter(s -> s.channel().equalsIgnoreCase(channel))
                .findFirst()
                .orElse(null);
        if (sender != null) {
            try {
                sender.send(receiver, scene, code);
            } catch (Exception e) {
                log.error("VerifyCodeService.sendCode 通道发送失败: receiver={}, channel={}, err={}",
                        receiver, channel, e.getMessage(), e);
                return new SendOutcome(false, 0, "验证码发送失败, 请稍后再试");
            }
        } else {
            log.info("[CODE-MOCK] receiver={} channel={} scene={} code={} (无下发通道, 日志 Mock)",
                    receiver, channel, scene, code);
        }

        codeStore.put(key, new CodeEntry(code, now));
        return new SendOutcome(true, SEND_COOLDOWN_SECONDS, null);
    }

    /**
     * 校验并一次性消费验证码. 不合法时抛 IllegalArgumentException (message 可直接给前端).
     *
     * @param receiver 接收方 (手机号 / 邮箱)
     * @param channel  PHONE / EMAIL
     * @param scene    LOGIN / REGISTER / RESET
     * @param code     用户提交的验证码
     */
    public void verifyCode(String receiver, String channel, String scene, String code) {
        String key = cacheKey(scene, channel, receiver);
        CodeEntry entry = codeStore.getIfPresent(key);
        if (entry == null) {
            throw new IllegalArgumentException("请先获取验证码");
        }
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("验证码不能为空");
        }
        if (entry.failCount >= MAX_VERIFY_FAILURES) {
            codeStore.invalidate(key);
            throw new IllegalArgumentException("错误次数过多, 验证码已作废, 请重新获取");
        }
        if (!entry.code.equals(code)) {
            entry.failCount++;
            throw new IllegalArgumentException("验证码错误");
        }
        codeStore.invalidate(key);  // 一次性消费
    }

    /**
     * 拼接验证码缓存键 (receiver 去首尾空格).
     */
    private static String cacheKey(String scene, String channel, String receiver) {
        return scene + ":" + channel + ":" + (receiver == null ? "" : receiver.trim());
    }
}
