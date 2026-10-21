package com.zifang.ctc.sso.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 「密钥不得进仓」守卫测试（交付物，禁止删除；本类只加测试，**不改任何生产行为**）。
 *
 * <p>背景：SSO 的 HS256 对称签名密钥曾以字面量写在
 * {@code z-ctc-sso/src/main/java/com/zifang/ctc/sso/config/SsoAutoConfiguration.java}
 * 的自动装配里，而该仓在 GitHub 上是 public ⇒ 任何人都能自己签发被生产接受的登录态，
 * 完整绕过 ctc / ops / opc / oss 四个域的鉴权。处置见
 * {@code 002_项目需求/001_特性需求/013_z-opc_周期性全量巡检/SSO签名密钥泄露收敛方案.md}。</p>
 *
 * <p><b>本守卫的三条口径</b>：</p>
 * <ol>
 *   <li><b>形态判据，不比对明文</b>：本类源码里**不存放那把密钥的字面量**（存了等于又抄一份进仓）。
 *       判据是「形」——① 已泄露值的 <b>sha256 前 8 位指纹</b>（只存指纹，不存明文）；
 *       ② 秘密位（secretKey / jwt.secret / password / …）上的<strong>字面量赋值</strong>；
 *       ③ 秘密位上<strong>长度异常</strong>的字面量。</li>
 *   <li><b>命中只报指纹，不报明文</b>：每条命中输出 {@code 路径:行号 sha256=xxxxxxxx len=N}，
 *       绝不打印命中的字符串本身。</li>
 *   <li><b>口径不漂移</b>：① 对全仓（含测试目录）生效，所以测试目录里那份拷贝也会被点名；
 *       ②③ 只对 {@code src/main} 生效——测试目录里的字符串常量不是生产配置，
 *       拿来当判据会全是噪声（断言消息、样例 key 都会被误伤）。</li>
 * </ol>
 *
 * <p><b>当前状态</b>：轮换（第 1 项）尚未落地，生产源码里那把字面量还在 ⇒
 * 本用例现在是<strong>红</strong>的，这是它该有的样子：红灯＝报警。
 * 轮换完成、字面量删除、改读配置后，本用例自动转绿，无需改本类。</p>
 */
class SsoSecretLiteralGuardTest {

    /**
     * 已泄露那把密钥的 <b>sha256 前 8 位</b>（十六进制）。
     * <p>只留指纹：指纹不可逆，拿它没法签 token，但它能让本守卫认出「同一把钥匙的任意拷贝」，
     * 包括测试目录里那份手抄的副本。</p>
     */
    private static final String KNOWN_LEAK_FINGERPRINT = "be7d8afd";

    /** 已泄露那把密钥的字节长度（同样只留长度，不留明文）。 */
    private static final int KNOWN_LEAK_LENGTH = 42;

    /**
     * Java 源码里「秘密位」字面量的失败门槛（字符数）。
     * <p>低于它的一律只当提示、不当失败：短串不可能是可用密钥，
     * 拿它当失败条件只会被断言消息、路径常量之类的东西反复误伤。</p>
     */
    private static final int JAVA_LITERAL_FAIL_LENGTH = 16;

    /** 配置文件里「秘密键」字面量的失败门槛（字符数），口径同 Java。 */
    private static final int CONFIG_VALUE_FAIL_LENGTH = 12;

    /** 秘密位标识符（大小写不敏感）。 */
    private static final Pattern SECRET_NAME = Pattern.compile(
            "(?i)\\b(secret|secret_?key|jwt_?secret|signing_?key|token_?secret|password|passwd|pwd|"
                    + "api_?key|access_?key|private_?key|credential|credentials)\\b");

    /** 构造器/类型名：名字里带这些词的，其字符串入参按「秘密位」看。 */
    private static final Pattern SECRET_TYPE = Pattern.compile(
            "(?i)[A-Za-z0-9_]*(secret|signing|jwt|token|cred|apikey|accesskey|privatekey)[A-Za-z0-9_]*");

    /** Java 字符串字面量。 */
    private static final Pattern JAVA_LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** 配置文件（yml / properties）里的键值对。 */
    private static final Pattern YAML_KV = Pattern.compile("^\\s*([A-Za-z0-9_.\\-\\[\\]]+)\\s*:\\s*(.*)$");
    private static final Pattern PROPS_KV = Pattern.compile("^\\s*([A-Za-z0-9_.\\-\\[\\]]+)\\s*=\\s*(.*)$");

    /** 占位符/样例值：一律不当密钥（外部化配置、写死的中文提示、无意义占位都归这里）。 */
    private static final Pattern PLACEHOLDER = Pattern.compile(
            "(?i)(\\$\\{|\\$\\{|ENC\\(|^<[^>]*>$|\\*+|x{3,}|changeme|changeit|example|dummy|placeholder"
                    + "|todo|^your[-_ ]?(secret|key|password|token)|^<your|^null$|^none$|^empty$)");

    // ================= 1. 口径一：已泄露值的指纹（全仓，含测试目录） =================

    @Test
    @DisplayName("口径一：全仓不得再出现那把已泄露密钥的任意拷贝（按 sha256 前 8 位指纹认人）")
    void noCopyOfKnownLeakedKeyAnywhereInRepo() {
        List<String> hits = new ArrayList<>();
        for (Path file : sourceFiles()) {
            for (Candidate c : candidatesOf(file)) {
                if (KNOWN_LEAK_FINGERPRINT.equals(fingerprint(c.value))) {
                    // 指纹撞车几乎不可能，但要是在长度上也对不上，说明这个常量被改坏了 —— 守卫本身得先可信。
                    if (c.value.length() != KNOWN_LEAK_LENGTH) {
                        fail("❌ 守卫自身不可信：指纹常量对应的长度是 " + c.value.length()
                                + "，但常量声明的预期长度是 " + KNOWN_LEAK_LENGTH
                                + "（指纹只认前 8 位，靠长度再确认一次）");
                    }
                    hits.add(locator(file, c) + " rule=known-fingerprint");
                }
            }
        }
        assertNoHit(hits, "已泄露密钥的拷贝");
    }

    // ================= 2. 口径二：生产源码里的「秘密位字面量赋值」 =================

    @Test
    @DisplayName("口径二：src/main 里不得有 secretKey / jwt.secret / password 之类的字面量赋值")
    void noSecretLiteralInProductionSources() {
        List<String> hits = new ArrayList<>();
        List<String> notices = new ArrayList<>();
        for (Path file : productionJavaFiles()) {
            for (Candidate c : candidatesOf(file)) {
                if (!isSecretPosition(c)) {
                    continue;
                }
                if (PLACEHOLDER.matcher(c.value).find() || c.value.trim().isEmpty()) {
                    continue;
                }
                String line = locator(file, c);
                if (c.value.length() >= JAVA_LITERAL_FAIL_LENGTH) {
                    hits.add(line + " rule=java-secret-literal");
                } else {
                    notices.add(line + " len=" + c.value.length() + " rule=java-secret-literal(short)");
                }
            }
        }
        if (!notices.isEmpty()) {
            System.out.println("[守卫提示] 秘密位上的短字面量（未达失败门槛，仅提示，不打印明文）：\n  "
                    + String.join("\n  ", notices));
        }
        assertNoHit(hits, "生产源码里的秘密位字面量");
    }

    // ================= 3. 口径三：配置文件里的「秘密键字面量」 =================

    @Test
    @DisplayName("口径三：application*/bootstrap* 配置文件里不得有秘密键的字面量值")
    void noSecretLiteralInConfigFiles() {
        List<String> hits = new ArrayList<>();
        for (Path file : configFiles()) {
            for (Candidate c : candidatesOf(file)) {
                if (!SECRET_NAME.matcher(c.key == null ? "" : c.key).find()) {
                    continue;
                }
                String value = c.value == null ? "" : c.value.trim();
                if (value.isEmpty() || PLACEHOLDER.matcher(value).find()) {
                    continue;
                }
                if (value.length() >= CONFIG_VALUE_FAIL_LENGTH) {
                    hits.add(locator(file, c) + " rule=config-secret-value");
                }
            }
        }
        assertNoHit(hits, "配置文件里的秘密键字面量");
    }

    // ================= 4. 守卫自身不许带明文 =================

    @Test
    @DisplayName("口径四：守卫自己不许把那把密钥的明文抄进来（只允许指纹）")
    void guardItselfCarriesNoPlaintext() {
        Path self = Paths.get("src/test/java/com/zifang/ctc/sso/config/SsoSecretLiteralGuardTest.java");
        assertTrue(Files.exists(self), "守卫源码不在预期位置: " + self);
        List<String> hits = new ArrayList<>();
        for (Candidate c : candidatesOf(self)) {
            if (KNOWN_LEAK_FINGERPRINT.equals(fingerprint(c.value))) {
                hits.add(locator(self, c) + " rule=self-plaintext");
            }
        }
        assertNoHit(hits, "守卫源码里的明文");
    }

    // ================= helpers =================

    /** 命中即红：只报指纹、路径、行号，绝不报明文。 */
    private static void assertNoHit(List<String> hits, String what) {
        if (hits.isEmpty()) {
            return;
        }
        List<String> sorted = new ArrayList<>(new LinkedHashSet<>(hits));
        Collections.sort(sorted);
        fail("❌ 守卫命中「" + what + "」" + sorted.size() + " 处（输出只含 sha256 前 8 位指纹 + 路径 + 行号，"
                + "不打印明文）：\n  " + String.join("\n  ", sorted)
                + "\n\n处置口径：密钥轮换 + 改读外部化配置（K8s Secret / 环境变量），"
                + "字面量不得留在仓库里；"
                + "已泄露那把即使换掉了也视同泄露，对应的 GitHub token / SSH key 必须一并轮换。");
    }

    /** 定位串：路径:行号 sha256=xxxxxxxx len=N。 */
    private static String locator(Path file, Candidate c) {
        return relative(file) + ":" + c.line + " sha256=" + fingerprint(c.value) + " len=" + c.value.length();
    }

    private static String relative(Path file) {
        Path root = repoRoot();
        return root.relativize(file.toAbsolutePath()).toString().replace('\\', '/');
    }

    /** sha256 前 8 位十六进制。 */
    private static String fingerprint(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4 && i < digest.length; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支持 SHA-256，守卫无法工作", e);
        }
    }

    /** 仓根：同时含 z-ctc-sso 与 pom.xml 的目录。 */
    private static Path repoRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 8 && dir != null; i++) {
            if (Files.isDirectory(dir.resolve("z-ctc-sso")) && Files.exists(dir.resolve("pom.xml"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("找不到仓根（同时含 z-ctc-sso 与 pom.xml 的目录）");
    }

    /** 全仓源码文件（main + test，Java 与配置文件）。 */
    private static List<Path> sourceFiles() {
        List<Path> out = new ArrayList<>();
        for (Path module : moduleDirs()) {
            for (String src : new String[]{"src/main", "src/test"}) {
                Path base = module.resolve(src);
                if (!Files.isDirectory(base)) {
                    continue;
                }
                out.addAll(walk(base, ".java", ".yml", ".yaml", ".properties"));
            }
        }
        return out;
    }

    /** 生产 Java 源码。 */
    private static List<Path> productionJavaFiles() {
        List<Path> out = new ArrayList<>();
        for (Path module : moduleDirs()) {
            Path base = module.resolve("src/main/java");
            if (Files.isDirectory(base)) {
                out.addAll(walk(base, ".java"));
            }
        }
        return out;
    }

    /** 生产配置文件（application* / bootstrap*）。 */
    private static List<Path> configFiles() {
        List<Path> out = new ArrayList<>();
        for (Path module : moduleDirs()) {
            Path base = module.resolve("src/main/resources");
            if (!Files.isDirectory(base)) {
                continue;
            }
            for (Path f : walk(base, ".yml", ".yaml", ".properties")) {
                String name = f.getFileName().toString();
                if (name.startsWith("application") || name.startsWith("bootstrap")) {
                    out.add(f);
                }
            }
        }
        return out;
    }

    /** 仓内各 maven 模块目录。 */
    private static List<Path> moduleDirs() {
        List<Path> out = new ArrayList<>();
        Path root = repoRoot();
        out.add(root);
        try (Stream<Path> children = Files.list(root)) {
            for (Path child : children.filter(Files::isDirectory).collect(Collectors.toList())) {
                if (Files.exists(child.resolve("pom.xml"))) {
                    out.add(child);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    private static List<Path> walk(Path base, String... suffixes) {
        List<String> allowed = Arrays.asList(suffixes);
        try (Stream<Path> stream = Files.walk(base)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> allowed.contains(suffixOf(p)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String suffixOf(Path p) {
        String n = p.getFileName().toString();
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i);
    }

    /** 抽出一份文件里所有「可能的字面量」候选（Java 字符串字面量 / 配置值）。 */
    private static List<Candidate> candidatesOf(Path file) {
        List<Candidate> out = new ArrayList<>();
        String suffix = suffixOf(file);
        if (".java".equals(suffix)) {
            List<String> lines = readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher m = JAVA_LITERAL.matcher(line);
                while (m.find()) {
                    out.add(new Candidate(i + 1, m.group(1), line));
                }
            }
            return out;
        }
        if (".yml".equals(suffix) || ".yaml".equals(suffix)) {
            List<String> lines = readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                Matcher m = YAML_KV.matcher(lines.get(i));
                if (m.matches()) {
                    out.add(new Candidate(i + 1, unquote(m.group(2)), lines.get(i), m.group(1)));
                }
            }
            return out;
        }
        if (".properties".equals(suffix)) {
            List<String> lines = readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                Matcher m = PROPS_KV.matcher(lines.get(i));
                if (m.matches()) {
                    out.add(new Candidate(i + 1, unquote(m.group(2)), lines.get(i), m.group(1)));
                }
            }
        }
        return out;
    }

    private static String unquote(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.length() >= 2 && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'")))) {
            v = v.substring(1, v.length() - 1);
        }
        return v;
    }

    /** 该字面量是否落在「秘密位」上：秘密位标识符赋值 / 秘密位类型的构造器入参。 */
    private static boolean isSecretPosition(Candidate c) {
        String line = c.text;
        if (line == null) {
            return false;
        }
        int at = line.indexOf(c.value);
        String before = at >= 0 ? line.substring(0, at) : line;
        if (SECRET_NAME.matcher(before).find()) {
            return true;
        }
        Matcher ctor = Pattern.compile("new\\s+(" + SECRET_TYPE.pattern() + ")\\s*\\(").matcher(line);
        return ctor.find();
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 一个字面量候选：行号 + 值 + 所在整行（配置文件另带键名）。 */
    private static final class Candidate {
        final int line;
        final String value;
        final String text;
        final String key;

        Candidate(int line, String value, String text) {
            this(line, value, text, null);
        }

        Candidate(int line, String value, String text, String key) {
            this.line = line;
            this.value = value;
            this.text = text;
            this.key = key;
        }
    }
}
