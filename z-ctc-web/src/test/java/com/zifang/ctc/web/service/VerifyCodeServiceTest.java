package com.zifang.ctc.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link VerifyCodeService} 回归测试.
 * <p>
 * 重点是验证码随机源：早前是 {@code (int)(Math.random() * 1_000_000)}，
 * {@code Math.random()} 走 java.util.Random 的 48 位 LCG，可预测。
 */
public class VerifyCodeServiceTest {

    private VerifyCodeService service;

    @BeforeEach
    void setUp() {
        service = new VerifyCodeService();
    }

    // ---------- 随机源 ----------

    @Test
    void testRandomSourceIsSecureRandom() throws Exception {
        Field f = VerifyCodeService.class.getDeclaredField("random");
        f.setAccessible(true);
        Object rng = f.get(service);
        assertNotNull(rng, "验证码必须走注入的随机源字段");
        assertTrue(rng instanceof SecureRandom,
                "验证码随机源必须是 SecureRandom，实际是 " + rng.getClass().getName()
                        + "（java.util.Random 是 48 位 LCG，可从观察值恢复状态）");
        assertFalse(rng instanceof Random && !(rng instanceof SecureRandom),
                "不得使用非密码学的 java.util.Random");
    }

    @Test
    void testCodeComesFromInjectedRandomSource() {
        // 证明 newCode() 确实读那个字段，而不是别处又调了一次 Math.random()
        final long seed = 42L;
        service.setRandomForTest(new SecureRandom() {
            private final Random delegate = new Random(seed);
            @Override
            public int nextInt(int bound) { return delegate.nextInt(bound); }
        });
        Random mirror = new Random(seed);   // 同一 seed 的镜像，按相同顺序取数
        assertEquals(String.format("%06d", mirror.nextInt(1_000_000)), service.newCode());
        assertEquals(String.format("%06d", mirror.nextInt(1_000_000)), service.newCode());
    }

    @Test
    void testCodeIsSixDigits() {
        for (int i = 0; i < 500; i++) {
            String code = service.newCode();
            assertEquals(6, code.length(), "验证码必须是 6 位: " + code);
            assertTrue(code.matches("\\d{6}"), "验证码必须全为数字: " + code);
        }
    }

    @Test
    void testCodesAreNotAllIdentical() {
        // 弱闸：500 次生成不应只有个位数不同值（能兜住"常量码"这类退化实现）
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            seen.add(service.newCode());
        }
        assertTrue(seen.size() > 400, "500 次生成只得到 " + seen.size() + " 个不同值，分布异常");
    }

    // ---------- 发送与冷却 ----------

    @Test
    void testSendCodeSucceedsAndSecondSendHitsCooldown() {
        VerifyCodeService.SendOutcome first = service.sendCode("13800000000",
                VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN);
        assertTrue(first.sent, "首次发送应成功");
        // 成功时返回的是"此后多少秒内不可再发"（= 冷却全长 60），不是"剩余 0"
        assertEquals(60L, first.cooldownSeconds, "成功后应告知冷却窗口长度");

        VerifyCodeService.SendOutcome second = service.sendCode("13800000000",
                VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN);
        assertFalse(second.sent, "60s 内重复发送应被冷却挡下");
        assertTrue(second.cooldownSeconds > 0, "冷却剩余秒数应 > 0，实际 " + second.cooldownSeconds);
    }

    @Test
    void testCooldownIsScopedBySceneAndChannel() {
        service.sendCode("13800000000", VerifyCodeService.CHANNEL_PHONE,
                VerifyCodeService.SCENE_LOGIN);
        // 不同 scene 不该被 LOGIN 的冷却挡住
        assertTrue(service.sendCode("13800000000", VerifyCodeService.CHANNEL_PHONE,
                VerifyCodeService.SCENE_REGISTER).sent, "不同 scene 应独立冷却");
    }

    @Test
    void testCooldownIsScopedByReceiver() {
        service.sendCode("13800000001", VerifyCodeService.CHANNEL_PHONE,
                VerifyCodeService.SCENE_LOGIN);
        assertTrue(service.sendCode("13800000002", VerifyCodeService.CHANNEL_PHONE,
                VerifyCodeService.SCENE_LOGIN).sent, "不同接收方应独立冷却");
    }

    // ---------- 校验 ----------

    @Test
    void testVerifyRequiresCodeFirst() {
        assertThrows(IllegalArgumentException.class, () -> service.verifyCode(
                "13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN, "000000"));
    }

    @Test
    void testVerifyRejectsEmptyCode() {
        service.sendCode("13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN);
        assertThrows(IllegalArgumentException.class, () -> service.verifyCode(
                "13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN, ""));
    }

    @Test
    void testVerifyConsumesCodeOnce() {
        // 固定随机源，让发出的码确定是 424242
        service.setRandomForTest(new SecureRandom() {
            private int done = 0;
            @Override public int nextInt(int bound) { return done++ == 0 ? 424242 : super.nextInt(bound); }
        });
        service.sendCode("13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN);

        service.verifyCode("13800000000", VerifyCodeService.CHANNEL_PHONE,
                VerifyCodeService.SCENE_LOGIN, "424242");
        // 一次性消费：同一个码再用必须失败
        assertThrows(IllegalArgumentException.class, () -> service.verifyCode(
                "13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN, "424242"),
                "验证码必须一次性消费");
    }

    @Test
    void testFiveWrongAttemptsVoidTheCode() {
        service.setRandomForTest(new SecureRandom() {
            private int done = 0;
            @Override public int nextInt(int bound) { return done++ == 0 ? 111111 : super.nextInt(bound); }
        });
        service.sendCode("13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN);

        // 连错 5 次
        for (int i = 0; i < 5; i++) {
            assertThrows(IllegalArgumentException.class, () -> service.verifyCode(
                    "13800000000", VerifyCodeService.CHANNEL_PHONE,
                    VerifyCodeService.SCENE_LOGIN, "999999"), "第 " + (i + 1) + " 次应判错");
        }
        // 第 6 次：即使报出正确码也应已作废
        assertThrows(IllegalArgumentException.class, () -> service.verifyCode(
                "13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN, "111111"),
                "错误 5 次后验证码应作废，正确码也不该再通过");
    }

    @Test
    void testWrongCodeDoesNotConsumeTheRealOne() {
        service.setRandomForTest(new SecureRandom() {
            private int done = 0;
            @Override public int nextInt(int bound) { return done++ == 0 ? 222222 : super.nextInt(bound); }
        });
        service.sendCode("13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN);
        assertThrows(IllegalArgumentException.class, () -> service.verifyCode(
                "13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN, "000001"));
        // 错的不作废对的
        service.verifyCode("13800000000", VerifyCodeService.CHANNEL_PHONE,
                VerifyCodeService.SCENE_LOGIN, "222222");
    }

    @Test
    void testVerifyIsScopedByScene() {
        service.setRandomForTest(new SecureRandom() {
            private int done = 0;
            @Override public int nextInt(int bound) { return done++ == 0 ? 333333 : super.nextInt(bound); }
        });
        service.sendCode("13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_RESET);
        // 用 LOGIN scene 校验应失败（该 scene 下没有发过码）
        assertThrows(IllegalArgumentException.class, () -> service.verifyCode(
                "13800000000", VerifyCodeService.CHANNEL_PHONE, VerifyCodeService.SCENE_LOGIN, "333333"));
    }
}
