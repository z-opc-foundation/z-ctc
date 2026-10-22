package com.zifang.ctc.admin.authz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.AntPathMatcher;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SSO 排除清单的边界守卫 —— 防止「宽前缀」把不该豁免的端点一起放行。
 *
 * <p>背景（卡 TASK-20261022-027）：{@code z-ctc-admin} 的 {@code sso.exclude-paths} 是
 * <b>纯路径前缀</b>匹配（{@link AntPathMatcher}，<b>没有 HTTP 方法维度</b>）。
 * 此前它只登记了 {@code /api/ctc/authn/login} 一个 authn 端点，
 * 而 {@code sso.intercept-paths: /api/**} 本该把其余 authn 端点全拦在闸门里 ——
 * 但实测 5 个端点全部匿名可达（POST 空体 5/5「到达处理器」），
 * 其中 {@code POST /api/ctc/authn/register} 只要 email + password 就能自助注册。</p>
 *
 * <p>拍板结论（用户 2026-10-22 23:4x）：</p>
 * <ul>
 *   <li>{@code send-code} / {@code phone-login} / {@code register} / {@code register-phone}
 *       —— 设计上<b>必须免登录</b> ⇒ 处置 = <b>显式登记进 exclude-paths</b>；</li>
 *   <li>{@code reset-password} —— <b>绝不该免登录</b>（匿名可改密 = 账号接管面）
 *       ⇒ 处置 = <b>留在闸门内</b> + 验证码 + 频率限制。</li>
 * </ul>
 *
 * <p><b>为什么本测试是反向钉</b>：豁免清单用宽前缀 {@code /api/ctc/authn/**} 写起来最省事，
 * 但它会把 {@code reset-password}（找回密码）与 {@code verify} / {@code whoami} /
 * {@code my-tenants} / {@code select-tenant} / {@code switch-tenant} 这些管理/身份端点
 * <b>连坐</b>一起豁免 —— 而 exclude-paths 没有方法维度，没法「再把某一个捞回来」。
 * ⇒ 本测试逐条拿每个 exclude 前缀去 match 每个受保护端点：
 * 任何人把宽前缀加回来，这些断言立刻红。</p>
 *
 * <p>断言口径复现 z-ctc {@code SsoInterceptor} 的真实匹配语义：
 * 命中 intercept 且未命中 exclude ⇒ 需要登录态；命中 exclude ⇒ 免登录。</p>
 */
class SsoExcludePathsBoundaryTest {

    /**
     * 设计上必须免登录的端点（拍板结论）—— 必须在 exclude-paths 里<b>被显式登记</b>。
     * 「行为上通着但清单里没登记」正是本卡的病根，不接受。
     */
    private static final List<String> DESIGN_EXEMPT_PATHS = Arrays.asList(
            "/api/ctc/authn/login",
            "/api/ctc/authn/send-code",
            "/api/ctc/authn/phone-login",
            "/api/ctc/authn/register",
            "/api/ctc/authn/register-phone");

    /**
     * 必须留在 SSO 闸门内的端点 —— <b>不得被任何 exclude 前缀连坐</b>。
     * <ul>
     *   <li>{@code reset-password}: 匿名可改密 = 账号接管面（拍板结论）；</li>
     *   <li>其余: 身份/租户管理端点（拿得到 userId 就能切租户、读租户列表）。</li>
     * </ul>
     */
    private static final List<String> MUST_STAY_PROTECTED_PATHS = Arrays.asList(
            "/api/ctc/authn/reset-password",
            "/api/ctc/authn/verify",
            "/api/ctc/authn/whoami",
            "/api/ctc/authn/my-tenants",
            "/api/ctc/authn/select-tenant",
            "/api/ctc/authn/switch-tenant",
            "/api/ctc/authn/logout");

    /** 读 z-ctc-admin 真正打包出去的那份 yml，而不是自己编一份。 */
    private static Map<String, Object> starterYaml() throws Exception {
        try (InputStream in = new ClassPathResource("application.yml").getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = (Map<String, Object>) new Yaml()
                    .load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return parsed;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> yamlList(Map<String, Object> yaml, String section, String key) {
        Object sso = yaml.get(section);
        assertNotNull(sso, "application.yml 里找不到 " + section + " 段");
        Object value = ((Map<String, Object>) sso).get(key);
        assertNotNull(value, "application.yml 里找不到 " + section + "." + key);
        List<String> out = new ArrayList<>();
        for (Object item : (List<Object>) value) {
            out.add(String.valueOf(item).trim());
        }
        return out;
    }

    private static boolean matchesAny(List<String> patterns, String path, AntPathMatcher m) {
        for (String pat : patterns) {
            if (m.match(pat, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验收项 3 的核心断言：四个自助端点（+ login）<b>必须</b>在 exclude-paths 里被显式登记。
     *
     * <p>写这一条是为了堵住「行为上通着但清单里没登记」——那正是本卡的病根：
     * 巡检量具读到的是<b>清单</b>，不是行为。</p>
     */
    @Test
    @DisplayName("设计上必须免登录的 5 个端点在 exclude-paths 里被显式登记")
    void designExemptEndpointsAreExplicitlyRegistered() throws Exception {
        Map<String, Object> yaml = starterYaml();
        List<String> exclude = yamlList(yaml, "sso", "exclude-paths");
        AntPathMatcher m = new AntPathMatcher();

        for (String p : DESIGN_EXEMPT_PATHS) {
            assertTrue(matchesAny(exclude, p, m),
                    p + " 设计上必须免登录, 却没有出现在 sso.exclude-paths 里 ⇒ "
                            + "巡检量具读不到豁免理由, 会被记成「漏网」。");
        }
    }

    /**
     * <b>反向钉</b>：exclude-paths 里<b>不得</b>存在任何会盖住受保护端点的宽前缀。
     *
     * <p>写法上不是只断言「某个已知路径没被排除」，而是<b>逐条</b>拿每个 exclude 前缀去
     * match 每个受保护端点 —— 这样任何人新加一条 {@code /api/ctc/authn/**} 之类的宽前缀
     * 都会立刻红。</p>
     */
    @Test
    @DisplayName("exclude-paths 里不得有会盖住 reset-password 等受保护端点的宽前缀")
    void excludePathsCarryNoBroadPrefixOverProtectedAuthnSurface() throws Exception {
        Map<String, Object> yaml = starterYaml();
        List<String> exclude = yamlList(yaml, "sso", "exclude-paths");
        AntPathMatcher m = new AntPathMatcher();

        for (String pat : exclude) {
            for (String protectedPath : MUST_STAY_PROTECTED_PATHS) {
                assertFalse(m.match(pat, protectedPath),
                        "exclude-paths 里的前缀 [" + pat + "] 会盖住受保护端点 " + protectedPath
                                + " ⇒ 该端点会免登录。exclude-paths 无 HTTP 方法维度，"
                                + "要豁免单个端点只能逐条登记，绝不能用 /api/ctc/authn/** 这类宽前缀。");
            }
        }
    }

    /**
     * 单独钉住 {@code reset-password}：它<b>绝不该</b>免登录（匿名可改密 = 账号接管面）。
     */
    @Test
    @DisplayName("reset-password 不被豁免（匿名可改密 = 账号接管面）")
    void resetPasswordIsNeverExempted() throws Exception {
        Map<String, Object> yaml = starterYaml();
        List<String> exclude = yamlList(yaml, "sso", "exclude-paths");
        AntPathMatcher m = new AntPathMatcher();
        assertFalse(matchesAny(exclude, "/api/ctc/authn/reset-password", m),
                "/api/ctc/authn/reset-password 命中 exclude-paths ⇒ 匿名可改密 = 账号接管面。"
                        + "它必须在 SSO 闸门内（已登录用户改密仍可用）。");
    }

    /**
     * 受保护端点必须<b>既命中 intercept 又不命中 exclude</b> ⇒ 会走 SSO 闸门。
     */
    @Test
    @DisplayName("受保护的 authn 端点全部落进 SSO 拦截范围且不被排除")
    void protectedAuthnEndpointsAreAuthenticated() throws Exception {
        Map<String, Object> yaml = starterYaml();
        List<String> intercept = yamlList(yaml, "sso", "intercept-paths");
        List<String> exclude = yamlList(yaml, "sso", "exclude-paths");
        AntPathMatcher m = new AntPathMatcher();

        for (String p : MUST_STAY_PROTECTED_PATHS) {
            assertTrue(matchesAny(intercept, p, m),
                    p + " 未命中 intercept-paths ⇒ preHandle 第一步就 return true 放行，等于没鉴权");
            assertFalse(matchesAny(exclude, p, m),
                    p + " 命中 exclude-paths ⇒ 免登录");
        }
    }

    /**
     * 对照组：正常登录流程<b>未被误伤</b>（验收项 8）。
     *
     * <p>{@code /api/ctc/authn/login} 必须仍在 exclude-paths 里 ——
     * 把它挡在闸门外 = 全站登录直接 401。</p>
     */
    @Test
    @DisplayName("对照组：正常登录 /api/ctc/authn/login 仍在 exclude-paths 里（未被误伤）")
    void normalLoginPathStaysExempt() throws Exception {
        Map<String, Object> yaml = starterYaml();
        List<String> exclude = yamlList(yaml, "sso", "exclude-paths");
        AntPathMatcher m = new AntPathMatcher();
        assertTrue(matchesAny(exclude, "/api/ctc/authn/login", m),
                "/api/ctc/authn/login 不在 exclude-paths 里 ⇒ 登录接口会被 SSO 闸门拦成 401，"
                        + "正常登录流程被误伤");
    }

    /**
     * 守卫自身的自检（反向钉的「牙齿」证明）。
     *
     * <p>如果哪天上面的断言因为写法问题而恒真（假绿），这条会红：
     * 它证明「把宽前缀 {@code /api/ctc/authn/**} 加回去」确实会盖住受保护端点。</p>
     */
    @Test
    @DisplayName("反向钉自检：宽前缀 /api/ctc/authn/** 一旦加回，守卫必须能盖住受保护端点")
    void broadPrefixWouldActuallyBreakTheGuard() throws Exception {
        Map<String, Object> yaml = starterYaml();
        List<String> exclude = yamlList(yaml, "sso", "exclude-paths");
        List<String> withBroadPrefix = new ArrayList<>(exclude);
        withBroadPrefix.add("/api/ctc/authn/**");

        AntPathMatcher m = new AntPathMatcher();
        List<String> covered = new ArrayList<>();
        for (String pat : withBroadPrefix) {
            for (String protectedPath : MUST_STAY_PROTECTED_PATHS) {
                if (m.match(pat, protectedPath)) {
                    covered.add(protectedPath);
                }
            }
        }
        assertFalse(covered.isEmpty(),
                "守卫自身失效: 宽前缀 /api/ctc/authn/** 竟然盖不住任何受保护端点 ⇒ "
                        + "上面的断言是假绿");
        assertTrue(covered.contains("/api/ctc/authn/reset-password"),
                "宽前缀必须会连坐 reset-password（这正是本卡要防的事），实际盖住的是 " + covered);
    }

    /**
     * 兜底：豁免清单里不得出现「authn 模块的整段通配」，除非是文档/健康检查这类非 authn 段。
     */
    @Test
    @DisplayName("exclude-paths 里没有 /api/ctc/authn/** 整段通配")
    void noWholeAuthnWildcardInExcludePaths() throws Exception {
        Map<String, Object> yaml = starterYaml();
        List<String> exclude = yamlList(yaml, "sso", "exclude-paths");
        assertFalse(exclude.contains("/api/ctc/authn/**"),
                "exclude-paths 里出现了 /api/ctc/authn/** 整段通配 ⇒ reset-password 等受保护端点被连坐豁免");
        assertFalse(exclude.contains("/api/ctc/**"),
                "exclude-paths 里出现了 /api/ctc/** 整段通配 ⇒ 整个 4A 中心的管理面被连坐豁免");
    }
}
