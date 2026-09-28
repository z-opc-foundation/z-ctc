package com.zifang.ctc.sso;

import com.zifang.util.core.jwt.Claims;
import com.zifang.util.core.jwt.Jwt;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 双向对拍尺（交付物，禁止删除；{@link LegacyJwtUtil} 是收编前实现的冻结副本，禁止改算法）。
 *
 * <p>背景：{@link JwtUtil} 已收编为 {@code com.zifang.util.core.jwt.Jwt}（z-util 1.0.13）的门面。
 * 线上历史 token 必须继续验得过、新 token 必须被各仓仍存的手抄旧逻辑验得过，所以本类同时持有两份实现：
 * <ul>
 *   <li>{@link LegacyJwtUtil} —— 收编前 {@code com.zifang.ctc.sso.JwtUtil} 的逐行冻结副本，
 *       只作为"历史 token 格式"的权威定义，不参与生产链路；</li>
 *   <li>{@link JwtUtil} / {@code Jwt} —— 现网实现。</li>
 * </ul>
 *
 * <p>覆盖：旧签→新验、新签→旧验、分段逐字节比对、base64url 口径、
 * 边界（空 claims／空串值／中文与 UTF-8 补充平面／8 万字符超长值／null 值／Boolean・Double・大整数／
 * 1 字符密钥・非 ASCII 密钥・1KB 密钥）、拒绝语义（过期・篡改 payload・篡改签名・错密钥・非三段式・
 * alg=HS512・alg=none・空串・null）。
 *
 * <p>以后有人想把 HS256 逻辑再抄一遍，先让本类全绿。
 */
class JwtCompatCrossCheckTest {

    /** 与 SsoAutoConfiguration 的现网签发密钥同形态（仅测试内使用，不外发）。 */
    private static final String SECRET = "ctc-secret-key-2024-secure-jwt-signing-key";

    // ================= 1. 双向可验 =================

    @Test
    void legacySignedTokenVerifiedByFacadeAndZUtil() {
        Map<String, Object> claims = productionClaimSet();
        String legacyToken = new LegacyJwtUtil(SECRET).generateToken(claims, 3600L);

        JwtUtil facade = new JwtUtil(SECRET);
        JwtUtil.VerificationResult byFacade = facade.verifyToken(legacyToken);
        assertTrue(byFacade.isValid(), "现网门面必须验过历史 token, 实际=" + byFacade.getErrorMessage());
        Map<String, Object> byLegacySelf = new LegacyJwtUtil(SECRET).verifyToken(legacyToken).getClaims();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            if (naiveParseSafe(entry.getValue())) {
                assertEquals(str(entry.getValue()), str(byFacade.getClaims().get(entry.getKey())),
                        "claim[" + entry.getKey() + "] 与历史签发值不一致");
                assertEquals(str(byLegacySelf.get(entry.getKey())), str(byFacade.getClaims().get(entry.getKey())),
                        "同一历史 token 的门面回读必须与历史实现一致: " + entry.getKey());
            }
        }
        assertEquals(Long.valueOf(2), byFacade.getClaims().get("accountType"),
                "整数 claim 类型必须与历史实现一致(Long, 不是 z-util 原生给的 Integer)");
        assertEquals("2", str(facade.getClaims(legacyToken).get("accountType")));
        // 唯一有意的读数差异：值里含英文逗号时，历史实现的暴力切分只能读回碎片（带残留引号），
        // 新实现按真实 JSON 读回完整值。签名/exp/iat 语义不受影响，现网 nickname 即此类。
        assertEquals("\"张小明", str(byLegacySelf.get("nickname")), "历史实现的碎片读数（既有事实）");
        assertEquals("张小明, 前端组", str(byFacade.getClaims().get("nickname")), "新实现读回完整值");
        assertEquals(byLegacySelf.get("exp"), byFacade.getClaims().get("exp"),
                "exp 的读数与类型必须与历史实现完全一致(Long)");
        assertEquals(byLegacySelf.get("iat"), byFacade.getClaims().get("iat"),
                "iat 的读数与类型必须与历史实现完全一致(Long)");

        Claims byZUtil = Jwt.parser().algorithm(Jwt.HS256).secret(SECRET).parse(legacyToken);
        assertEquals("u_10001", str(byZUtil.get("username")));
        assertEquals("张小明, 前端组", str(byZUtil.get("nickname")));
        assertNotNull(byZUtil.iat(), "历史 token 的 iat 必须能按数值读出");
        assertNotNull(byZUtil.exp(), "历史 token 的 exp 必须能按数值读出");
        assertTrue(byZUtil.exp() > byZUtil.iat());
    }

    @Test
    void facadeSignedTokenVerifiedByLegacy() {
        JwtUtil facade = new JwtUtil(SECRET);
        LegacyJwtUtil legacy = new LegacyJwtUtil(SECRET);
        for (Map<String, Object> claims : allCrossCheckClaimSets()) {
            String newToken = sameSecondPair(claims, SECRET)[1];
            JwtUtil.VerificationResult v = legacy.verifyToken(newToken);
            assertTrue(v.isValid(), "历史实现必须验得出新签 token, claims=" + keySetOf(claims)
                    + " 实际=" + v.getErrorMessage());
            if (allNaiveParseSafe(claims)) {
                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    assertEquals(str(entry.getValue()), str(v.getClaims().get(entry.getKey())),
                            "历史实现回读 claim[" + entry.getKey() + "] 不一致");
                }
            }
            // exp 在两侧都读得到且同值同类型 → 过期语义不因收编而漂移
            assertEquals(v.getClaims().get("exp"), facade.verifyToken(newToken).getClaims().get("exp"),
                    "exp 读数/类型必须一致, claims=" + keySetOf(claims));
            assertTrue(facade.verifyToken(newToken).isValid(), "门面自读失败");
        }
    }

    @Test
    void zUtilNativeSignedTokenVerifiedByLegacy() {
        Claims claims = new Claims().sub("alice").put("uid", "42").expireIn(3600L);
        String token = Jwt.builder().algorithm(Jwt.HS256).secret(SECRET).claims(claims).build();

        JwtUtil.VerificationResult v = new LegacyJwtUtil(SECRET).verifyToken(token);
        assertTrue(v.isValid(), "历史实现读不出 z-util 原生 token: " + v.getErrorMessage());
        assertEquals("alice", str(v.getClaims().get("sub")));
        assertEquals("42", str(v.getClaims().get("uid")));
    }

    // ================= 2. 分段逐字节比对 =================

    /**
     * 结论（本用例把它钉死）：
     * <ul>
     *   <li>payload 段：逐字节相同（门面刻意走 HashMap 迭代顺序，复刻收编前的序列化结果）；</li>
     *   <li>header 段：语义相同、字面不同 —— 收编前 HashMap 迭代出 {@code {"typ":"JWT","alg":"HS256"}}，
     *       z-util 固定 {@code {"alg":"HS256","typ":"JWT"}}；验签取字面段，故互验不受影响；</li>
     *   <li>整串 token 因此不同（签名覆盖的原文不同），但两个方向都验得过（用例 1）。</li>
     * </ul>
     */
    @Test
    void segmentLevelByteComparison() {
        for (Map<String, Object> claims : allCrossCheckClaimSets()) {
            String[] pair = sameSecondPair(claims, SECRET);
            String legacyHeader = segment(pair[0], 0);
            String legacyPayload = segment(pair[0], 1);
            String newHeader = segment(pair[1], 0);
            String newPayload = segment(pair[1], 1);

            assertEquals(legacyPayload, newPayload, "payload 段必须逐字节相同, claims=" + keySetOf(claims));
            assertEquals("{\"typ\":\"JWT\",\"alg\":\"HS256\"}", decodeJsonText(legacyHeader),
                    "历史 header 字面");
            assertEquals("{\"alg\":\"HS256\",\"typ\":\"JWT\"}", decodeJsonText(newHeader),
                    "z-util header 字面");
            assertEquals("HS256", legacyDecode(legacyHeader).get("alg"));
            assertEquals("HS256", legacyDecode(newHeader).get("alg"), "历史解析器读新 header 也须拿到 HS256");
            assertEquals("JWT", legacyDecode(newHeader).get("typ"));
            assertFalse(pair[0].equals(pair[1]), "header 键序不同 → 整串不同（预期）");
            assertEquals(3, pair[1].split("\\.").length, "标准三段式");
        }
    }

    @Test
    void base64UrlHandlingIdentical() {
        String[] pair = sameSecondPair(productionClaimSet(), SECRET);
        for (String token : pair) {
            for (String seg : token.split("\\.")) {
                assertFalse(seg.contains("+"), "不得出现标准 base64 的 +: " + seg);
                assertFalse(seg.contains("/"), "不得出现标准 base64 的 /: " + seg);
                assertFalse(seg.contains("="), "不得有 base64 padding: " + seg);
            }
            // HS256 签名 32 字节 → 无填充 base64url 43 字符
            assertEquals(43, segment(token, 2).length(), token);
            // 段必须是 url 字母表
            assertTrue(segment(token, 2).matches("[A-Za-z0-9_-]{43}"), "签名字符集非 url-safe");
        }
    }

    // ================= 3. 边界 =================

    @Test
    void boundaryClaimSetsAndSecretsCrossVerify() {
        // 空 claims
        assertCrossReadable(new HashMap<String, Object>(), SECRET);
        // 空串值
        Map<String, Object> empty = new HashMap<String, Object>();
        empty.put("username", "");
        empty.put("nickname", "");
        empty.put("tenantCode", "");
        assertCrossReadable(empty, SECRET);
        // 中文 / UTF-8 补充平面 / 全角标点
        Map<String, Object> utf8 = new HashMap<String, Object>();
        utf8.put("nickname", "张三丰（研发部）🙂");
        utf8.put("reason", "为「客户甲」开通 7 天试用");
        utf8.put("tenantCode", "租户A");
        assertCrossReadable(utf8, SECRET);
        assertCrossReadable(utf8, "密钥-äöü");
        // 超长值（8 万字符）
        Map<String, Object> huge = new HashMap<String, Object>();
        char[] chars = new char[80000];
        Arrays.fill(chars, 'a');
        huge.put("blob", new String(chars));
        assertCrossReadable(huge, SECRET);
        // Boolean / Double / 大整数 / 负数
        Map<String, Object> typed = new HashMap<String, Object>();
        typed.put("tempAccount", Boolean.TRUE);
        typed.put("ratio", Double.valueOf(0.5));
        typed.put("big", Long.valueOf(9007199254740993L));
        typed.put("neg", Integer.valueOf(-7));
        assertCrossReadable(typed, SECRET);
        // 1 字符密钥 / 1KB 密钥（HMAC 对密钥长度不敏感，双方都要成立）
        assertCrossReadable(typed, "k");
        assertCrossReadable(typed, repeat("s", 1024));
        assertCrossReadable(typed, repeat("密", 64));
    }

    /**
     * null 值 claim：历史写出 {@code "openedBy":null}，历史解析器把它读回成字符串 "null"，
     * z-util 读回真正的 null。字面格式一致（互验通过），仅回读类型不同 —— 现网无人读 openedBy。
     */
    @Test
    void nullClaimValueCrossVerifyWithTypeNote() {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("username", "u1");
        claims.put("openedBy", null);

        String legacyToken = new LegacyJwtUtil(SECRET).generateToken(claims, 3600L);
        String newToken = new JwtUtil(SECRET).generateToken(claims, 3600L);
        assertTrue(new JwtUtil(SECRET).verifyToken(legacyToken).isValid(), "新实现读不出含 null claim 的历史 token");
        assertTrue(new LegacyJwtUtil(SECRET).verifyToken(newToken).isValid(), "历史实现读不出新签含 null claim 的 token");
        assertEquals(segment(legacyToken, 1), segment(newToken, 1), "payload 段须逐字节相同");

        assertEquals("null", str(new LegacyJwtUtil(SECRET).verifyToken(legacyToken).getClaims().get("openedBy")),
                "历史实现把 null 读成字符串 \"null\"（既有事实）");
        assertNull(new JwtUtil(SECRET).getClaims(legacyToken).get("openedBy"), "新实现读成真正的 null");
    }

    // ================= 4. 拒绝语义（不许放宽） =================

    @Test
    void expiredTokensRejectedBothWays() {
        Map<String, Object> claims = productionClaimSet();
        String expiredLegacy = new LegacyJwtUtil(SECRET).generateToken(claims, -3600L);
        String expiredNew = new JwtUtil(SECRET).generateToken(claims, -3600L);

        assertFalse(new JwtUtil(SECRET).verifyToken(expiredLegacy).isValid(), "历史过期 token 新实现必须拒");
        assertFalse(new LegacyJwtUtil(SECRET).verifyToken(expiredNew).isValid(), "新签过期 token 历史实现必须拒");
        assertFalse(new LegacyJwtUtil(SECRET).verifyToken(expiredLegacy).isValid());
        assertFalse(new JwtUtil(SECRET).verifyToken(expiredNew).isValid());
        assertEquals("令牌已过期", new JwtUtil(SECRET).verifyToken(expiredLegacy).getErrorMessage(),
                "过期文案须保持收编前中文口径");
        assertNull(new JwtUtil(SECRET).getClaims(expiredLegacy));
    }

    @Test
    void tamperedAndWrongKeyTokensRejectedBothWays() {
        Map<String, Object> claims = productionClaimSet();
        String legacyToken = new LegacyJwtUtil(SECRET).generateToken(claims, 3600L);
        assertTrue(new JwtUtil(SECRET).verifyToken(legacyToken).isValid(), "前置条件不成立");

        String swapped = segment(legacyToken, 0) + "." + encodeUrl(
                "{\"userId\":\"999\",\"accountType\":2,\"username\":\"attacker\",\"iat\":1,\"exp\":9999999999}"
                        .getBytes(StandardCharsets.UTF_8)) + "." + segment(legacyToken, 2);
        assertFalse(new JwtUtil(SECRET).verifyToken(swapped).isValid(), "换 payload 不换签名必须拒");
        assertFalse(new LegacyJwtUtil(SECRET).verifyToken(swapped).isValid());

        String flippedSig = segment(legacyToken, 0) + "." + segment(legacyToken, 1)
                + "." + flip(segment(legacyToken, 2));
        assertFalse(new JwtUtil(SECRET).verifyToken(flippedSig).isValid(), "改签名必须拒");
        assertFalse(new LegacyJwtUtil(SECRET).verifyToken(flippedSig).isValid());

        assertFalse(new JwtUtil("other-secret").verifyToken(legacyToken).isValid(), "错密钥必须拒");
        assertFalse(new LegacyJwtUtil("other-secret").verifyToken(legacyToken).isValid());

        String newToken = new JwtUtil(SECRET).generateToken(claims, 3600L);
        assertFalse(new JwtUtil("other-secret").verifyToken(newToken).isValid());
        assertFalse(new LegacyJwtUtil("other-secret").verifyToken(newToken).isValid());
    }

    @Test
    void malformedAndForeignAlgTokensRejectedBothWays() {
        String[] junk = {"", "abc", "a.b", "a.b.c.d", ".....", "not!base64.url!seg", "..",
                encodeUrl("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)) + ".."};
        for (String token : junk) {
            assertFalse(new JwtUtil(SECRET).verifyToken(token).isValid(), "门面误判: [" + token + "]");
            assertFalse(new LegacyJwtUtil(SECRET).verifyToken(token).isValid(), "历史口径被改变: [" + token + "]");
            assertNull(new JwtUtil(SECRET).getClaims(token));
        }
        assertFalse(new JwtUtil(SECRET).verifyToken(null).isValid(), "null token 必须拒而不是抛");
        assertFalse(new LegacyJwtUtil(SECRET).verifyToken(null).isValid());

        // HS512 / alg=none 都要拒（门面把算法钉死在 HS256，且不做 alg 协商）
        String hs512 = Jwt.builder().algorithm(Jwt.HS512).secret(SECRET)
                .claims(new Claims().put("username", "u").expireIn(3600L)).build();
        assertFalse(new JwtUtil(SECRET).verifyToken(hs512).isValid(), "HS512 token 必须拒");
        assertFalse(new LegacyJwtUtil(SECRET).verifyToken(hs512).isValid());

        String none = encodeUrl("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
                + "." + encodeUrl("{\"username\":\"u\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8)) + ".";
        assertFalse(new JwtUtil(SECRET).verifyToken(none).isValid(), "alg=none 必须拒");
        assertFalse(new LegacyJwtUtil(SECRET).verifyToken(none).isValid());
    }

    /**
     * 刻意保留（并上报）的差异：历史实现把 List claim 用 {@code toString()} 写成非法 JSON
     * {@code [user, admin]}，这类历史 token 新实现读不出来（判无效）。
     * 现网签发链路（AuthnController.issueToken / TokenRefreshService / TempAccountService）
     * 只写 String/Integer/Boolean，不产生这种 token，故不影响线上数据。
     */
    @Test
    void legacyListClaimTokenUnreadableIsDocumentedDivergence() {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("username", "u1");
        claims.put("roles", Arrays.asList("user", "admin"));
        String legacyToken = new LegacyJwtUtil(SECRET).generateToken(claims, 3600L);

        JwtUtil.VerificationResult oldRead = new LegacyJwtUtil(SECRET).verifyToken(legacyToken);
        assertTrue(oldRead.isValid(), "历史实现自身仍判有效");
        assertEquals("u1", str(oldRead.getClaims().get("username")));
        assertEquals("[user", str(oldRead.getClaims().get("roles")),
                "历史实现的 List claim 回读本来就是碎的");

        assertFalse(new JwtUtil(SECRET).verifyToken(legacyToken).isValid(),
                "新实现判该脏格式 token 无效（不兼容脏格式，属收紧，不放宽）");

        // 门面自己写的 List claim 是合法 JSON，历史实现仍能验签（回读仍碎）
        String newToken = new JwtUtil(SECRET).generateToken(claims, 3600L);
        assertTrue(new LegacyJwtUtil(SECRET).verifyToken(newToken).isValid());
        assertTrue(Jwt.parser().algorithm(Jwt.HS256).secret(SECRET).parse(newToken)
                .get("roles") instanceof List, "新实现写的是合法 JSON 数组");
    }

    /**
     * 实测结论（不靠猜）：字符串值里含英文逗号时，历史实现的暴力切分只会把<b>该 claim 的值</b>切成碎片
     * 并丢掉紧随其后的碎片，{@code exp}/{@code iat} 等其它键仍能正常读到 —— 所以两侧过期语义一致，
     * 不存在"历史 token 因切分丢 exp 而被永久接受"的情况（若真丢，本用例的 exp 断言会红）。
     */
    @Test
    void commaInValueManglesOnlyThatClaimNotExpiry() {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("reason", "a,b");
        claims.put("username", "u1");
        LegacyJwtUtil legacy = new LegacyJwtUtil(SECRET);
        JwtUtil facade = new JwtUtil(SECRET);
        String fresh = legacy.generateToken(claims, 3600L);
        String expired = legacy.generateToken(claims, -3600L);

        Map<String, Object> legacyRead = legacy.verifyToken(fresh).getClaims();
        assertTrue(legacy.verifyToken(fresh).isValid(), "前置条件: 未过期");
        assertEquals("\"a", str(legacyRead.get("reason")), "历史实现把含逗号的值切成碎片（既有事实）");
        assertNotNull(legacyRead.get("exp"), "历史实现仍能读到 exp");
        assertFalse(legacy.verifyToken(expired).isValid(), "历史实现判过期拒绝");
        assertFalse(facade.verifyToken(expired).isValid(), "新实现同样判过期拒绝");
        assertTrue(facade.verifyToken(fresh).isValid(), "新实现读得出去含逗号值的历史 token");
        assertEquals("a,b", str(facade.getClaims(fresh).get("reason")), "新实现读回完整值");
    }

    // ================= helpers =================

    /**
     * AuthnController.issueToken 的真实 claims 形状（全 String + 一个 Integer accountType）。
     */
    private static Map<String, Object> productionClaimSet() {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("userId", "10001");
        claims.put("username", "u_10001");
        claims.put("nickname", "张小明, 前端组");
        claims.put("staffId", "SN-0001");
        claims.put("tenantCode", "t_acme");
        claims.put("domainCode", "d_shop");
        claims.put("accountType", Integer.valueOf(2));
        return claims;
    }

    private static List<Map<String, Object>> allCrossCheckClaimSets() {
        List<Map<String, Object>> sets = new ArrayList<Map<String, Object>>();
        sets.add(productionClaimSet());
        sets.add(new HashMap<String, Object>());
        Map<String, Object> one = new HashMap<String, Object>();
        one.put("username", "john.doe");
        sets.add(one);
        Map<String, Object> refresh = new HashMap<String, Object>();
        refresh.put("tokenType", "refresh");
        refresh.put("refreshId", "3f1b-uuid-ish");
        refresh.put("userId", "1");
        refresh.put("username", "u");
        refresh.put("nickname", "n");
        sets.add(refresh);
        Map<String, Object> temp = new HashMap<String, Object>();
        temp.put("tempAccount", Boolean.TRUE);
        temp.put("recordId", "rec-1");
        temp.put("openedBy", "admin");
        temp.put("reason", "为 XX 客户开通 7 天试用");
        sets.add(temp);
        Map<String, Object> utf8 = new HashMap<String, Object>();
        utf8.put("nickname", "李雷（研发）🙂");
        utf8.put("tenantCode", "租户A");
        sets.add(utf8);
        Map<String, Object> emptyVal = new HashMap<String, Object>();
        emptyVal.put("username", "");
        emptyVal.put("tenantCode", "");
        sets.add(emptyVal);
        return sets;
    }

    /**
     * 双向可读 + payload 段逐字节相同 + 同一历史 token 两侧回读值一致。
     */
    private static void assertCrossReadable(Map<String, Object> claims, String secret) {
        JwtUtil facade = new JwtUtil(secret);
        LegacyJwtUtil legacy = new LegacyJwtUtil(secret);
        String[] pair = sameSecondPair(claims, secret);
        String legacyToken = pair[0];
        String newToken = pair[1];

        assertTrue(facade.verifyToken(legacyToken).isValid(),
                "新实现读不出历史 token, claims=" + keySetOf(claims));
        assertTrue(legacy.verifyToken(newToken).isValid(),
                "历史实现读不出新签 token, claims=" + keySetOf(claims));

        Map<String, Object> fromOld = legacy.verifyToken(legacyToken).getClaims();
        Map<String, Object> fromNew = facade.verifyToken(legacyToken).getClaims();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            assertEquals(str(fromOld.get(entry.getKey())), str(fromNew.get(entry.getKey())),
                    "同一历史 token 新旧回读不一致: " + entry.getKey());
        }
        assertEquals(segment(legacyToken, 1), segment(newToken, 1),
                "payload 段必须逐字节相同, claims=" + keySetOf(claims));
    }

    /**
     * iat/exp 由时钟决定，跨秒会让两份 token 字面不同；重试直到两侧落在同一秒。
     */
    private static String[] sameSecondPair(Map<String, Object> claims, String secret) {
        String legacyToken = null;
        String newToken = null;
        for (int i = 0; i < 10; i++) {
            long expect = System.currentTimeMillis() / 1000;
            legacyToken = new LegacyJwtUtil(secret).generateToken(claims, 3600L);
            newToken = new JwtUtil(secret).generateToken(claims, 3600L);
            if (segment(legacyToken, 1).equals(segment(newToken, 1))
                    && Long.parseLong(str(legacyDecode(segment(legacyToken, 1)).get("iat"))) == expect) {
                return new String[]{legacyToken, newToken};
            }
        }
        return new String[]{legacyToken, newToken};
    }

    private static String segment(String token, int idx) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        return parts.length > idx ? parts[idx] : "";
    }

    private static String encodeUrl(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String flip(String sig) {
        return (sig.charAt(0) == 'a' ? "b" : "a") + sig.substring(1);
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static String keySetOf(Map<String, Object> claims) {
        return String.join(",", new TreeSet<String>(claims.keySet()));
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /**
     * 历史实现的暴力 JSON 解析（split 逗号 + 剥一层引号）只对"值里不含英文逗号/冒号/引号"的 claim 忠实，
     * 且任何一个含逗号的值会连带把排在它之后的最后一个键也带歪（尾巴上挂个 {@code }}）。
     * 因此含这类字符的 claims 只断言"能验签 + 能读到 exp"，不断言逐键回读一致。
     */
    private static boolean naiveParseSafe(Object value) {
        if (value == null) {
            return false;
        }
        String s = String.valueOf(value);
        return s.indexOf(',') < 0 && s.indexOf(':') < 0 && s.indexOf('"') < 0;
    }

    private static boolean allNaiveParseSafe(Map<String, Object> claims) {
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            if (!naiveParseSafe(entry.getKey()) || !naiveParseSafe(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /** 用历史实现的解析器读段（保持"历史格式权威定义"）。 */
    private static Map<String, Object> legacyDecode(String base64UrlSegment) {
        return LegacyJwtUtil.decodeSegment(base64UrlSegment);
    }

    private static String decodeJsonText(String base64UrlSegment) {
        return new String(Base64.getUrlDecoder().decode(base64UrlSegment), StandardCharsets.UTF_8);
    }

    // ================= 冻结副本：收编前的 JwtUtil（禁止改动算法实现） =================

    /**
     * 收编前 {@code com.zifang.ctc.sso.JwtUtil} 的逐行冻结副本（HS256 + 手写 JSON + base64url）。
     * <p>它是"线上历史 token 格式"的唯一权威定义：只允许整体替换，不允许局部修改。
     */
    static final class LegacyJwtUtil {

        private static final String HMAC_SHA256 = "HmacSHA256";
        private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
        private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

        private final String secretKey;

        LegacyJwtUtil(String secretKey) {
            this.secretKey = secretKey;
        }

        static Map<String, Object> decodeSegment(String encoded) {
            return new LegacyJwtUtil("").decodeJson(encoded);
        }

        String generateToken(Map<String, Object> claims, long expiresIn) {
            try {
                Map<String, Object> header = new HashMap<String, Object>();
                header.put("alg", "HS256");
                header.put("typ", "JWT");
                String encodedHeader = encodeJson(header);

                Map<String, Object> payload = new HashMap<String, Object>(claims);
                payload.put("iat", System.currentTimeMillis() / 1000);
                payload.put("exp", System.currentTimeMillis() / 1000 + expiresIn);
                String encodedPayload = encodeJson(payload);

                String data = encodedHeader + "." + encodedPayload;
                String signature = hmacSha256(data, secretKey);

                return data + "." + signature;
            } catch (Exception e) {
                throw new RuntimeException("生成JWT失败", e);
            }
        }

        JwtUtil.VerificationResult verifyToken(String token) {
            try {
                String[] parts = token.split("\\.");
                if (parts.length != 3) {
                    return new JwtUtil.VerificationResult(false, null, "JWT格式不正确");
                }

                String encodedHeader = parts[0];
                String encodedPayload = parts[1];
                String signature = parts[2];

                String data = encodedHeader + "." + encodedPayload;
                String expectedSignature = hmacSha256(data, secretKey);
                if (!signature.equals(expectedSignature)) {
                    return new JwtUtil.VerificationResult(false, null, "签名验证失败");
                }

                Map<String, Object> header = decodeJson(encodedHeader);
                if (!"HS256".equals(header.get("alg"))) {
                    return new JwtUtil.VerificationResult(false, null, "不支持的算法");
                }

                Map<String, Object> payload = decodeJson(encodedPayload);

                long exp = getLongClaim(payload, "exp");
                if (exp > 0 && System.currentTimeMillis() / 1000 > exp) {
                    return new JwtUtil.VerificationResult(false, null, "令牌已过期");
                }

                return new JwtUtil.VerificationResult(true, payload, null);
            } catch (Exception e) {
                return new JwtUtil.VerificationResult(false, null, "验证过程发生异常: " + e.getMessage());
            }
        }

        private String hmacSha256(String data, String secret) {
            try {
                Mac mac = Mac.getInstance(HMAC_SHA256);
                SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
                mac.init(keySpec);
                byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
                return BASE64_URL_ENCODER.encodeToString(hash);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException("签名失败", e);
            }
        }

        private String encodeJson(Map<String, Object> json) {
            String jsonString = toJsonString(json);
            return BASE64_URL_ENCODER.encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));
        }

        private Map<String, Object> decodeJson(String encoded) {
            byte[] bytes = BASE64_URL_DECODER.decode(encoded);
            String jsonString = new String(bytes, StandardCharsets.UTF_8);
            return fromJsonString(jsonString);
        }

        private String toJsonString(Map<String, Object> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",");
                }

                sb.append("\"").append(entry.getKey()).append("\":");

                Object value = entry.getValue();
                if (value == null) {
                    sb.append("null");
                } else if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(value);
                } else if (value instanceof List) {
                    sb.append(value.toString());
                } else {
                    sb.append("\"").append(value.toString()).append("\"");
                }

                first = false;
            }
            sb.append("}");
            return sb.toString();
        }

        private Map<String, Object> fromJsonString(String json) {
            Map<String, Object> map = new HashMap<String, Object>();
            json = json.trim();

            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                String[] keyValues = json.split(",");

                for (String keyValue : keyValues) {
                    keyValue = keyValue.trim();
                    if (keyValue.isEmpty()) {
                        continue;
                    }

                    int colonIndex = keyValue.indexOf(":");
                    if (colonIndex > 0) {
                        String key = keyValue.substring(0, colonIndex).trim();
                        String value = keyValue.substring(colonIndex + 1).trim();

                        if (key.startsWith("\"") && key.endsWith("\"")) {
                            key = key.substring(1, key.length() - 1);
                        }

                        Object parsedValue;
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            parsedValue = value.substring(1, value.length() - 1);
                        } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                            parsedValue = Boolean.valueOf(value);
                        } else {
                            try {
                                parsedValue = Long.valueOf(value);
                            } catch (NumberFormatException e) {
                                try {
                                    parsedValue = Double.valueOf(value);
                                } catch (NumberFormatException ex) {
                                    parsedValue = value;
                                }
                            }
                        }

                        map.put(key, parsedValue);
                    }
                }
            }

            return map;
        }

        private long getLongClaim(Map<String, Object> claims, String claimName) {
            Object value = claims.get(claimName);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            try {
                return Long.parseLong(value.toString());
            } catch (Exception e) {
                return -1;
            }
        }
    }
}
