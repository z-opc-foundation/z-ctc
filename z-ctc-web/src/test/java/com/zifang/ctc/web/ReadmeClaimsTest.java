package com.zifang.ctc.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * z-ctc README 定量主张自守卫 —— README 里写死的版本、模块清单、Controller 名单、
 * API 前缀、端口、配置键、环境变量、文档目录，必须能从源码重算出来；
 * README 改了就红，代码没动就不红。
 *
 * <p><b>为什么要有这一层</b>：本仓 README 把「当前版本 1.0.4」「12 个 Controller」
 * 「12 条 API 前缀」「reactor 7 个模块」「默认端口 8888」「10 个 SSO 配置键」
 * 「数据源 5 个环境变量」「9 个 k8s 清单」「4 个前端工程」「3 种 TokenService 实现」
 * 写成了硬数字，还把一个**全仓无定义**的环境变量组（{@code CTC_DB_*}）写进了
 * 数据源配置表 —— 那正是历史上导致生产库认证失败的那处漂移。没有任何尺子去问代码
 * 「现在还是吗」，这些数字的保质期就只剩上一次编辑 README 的那一刻。</p>
 *
 * <p><b>口径四条（都是踩过的坑）</b>：</p>
 * <ol>
 *   <li><b>事实对象只装源码侧事实</b>（{@link Facts}）—— README 侧的数值一律在核对
 *       <b>当下</b>从被核对的那份 README 里现解析。两边都从同一份 README 取数，
 *       尺子就成了恒等式比较，README 改成 8889 尺子照样绿；</li>
 *   <li><b>主张抠不到就红</b>：正则没匹配上不许静默跳过，那正是空跑；</li>
 *   <li><b>按小节名 / 表格字段定位，不按数字定位</b>：短数字是长数字的子串
 *       （「4 个」⊂「14 个」），按数字定位会匹配到别人的行上；</li>
 *   <li><b>README 标题带 emoji</b>：定位小节要剥掉 emoji 后<b>标题文字全等匹配</b>，
 *       不能用 {@code contains}（「模块结构」会误命中「模块结构与约定」）。</li>
 * </ol>
 *
 * <p>数值不等 = 红，且把<b>重算值</b>打进消息（红了要知道真数是多少，否则只是又一处猜）。
 * 核对结果<b>一次全报</b>（收集齐所有不一致再抛），撞停会把尺子退化成「一次只说一句」；
 * 尺子的牙口由 {@link #尺子的牙口()} 用合成 README 证明，
 * 而「不许冤枉健康页」由 {@link #不许冤枉健康页()} 本身证明。</p>
 */
class ReadmeClaimsTest {

    /** 仓根标志：artifactId 命中 + packaging=pom + 真带 <module>（子模块 pom 的 parent 里也有同名 artifactId）。 */
    private static final String ROOT_ARTIFACT_ID = "z-ctc";

    /** 源码侧路径一律相对仓根写死，不按目录名统计（多模块仓里同名目录不止一处）。 */
    private static final String WEB_API_DIR = "z-ctc-web/src/main/java/com/zifang/ctc/web/api";
    private static final String SSO_MAIN = "z-ctc-sso/src/main/java";
    private static final String SSO_PROPERTIES = "z-ctc-sso/src/main/java/com/zifang/ctc/sso/config/SsoProperties.java";
    private static final String ADMIN_YML = "z-ctc-admin/src/main/resources/application.yml";
    private static final String ADMIN_DEV_YML = "z-ctc-admin/src/main/resources/application-dev.yml";
    private static final String ADMIN_POM = "z-ctc-admin/pom.xml";
    private static final String ROOT_POM = "pom.xml";
    private static final String K8S_DIR = "deploy/k8s";
    private static final String FRONTEND_DIR = "_frontend";
    private static final String DOC_DIR = "_doc";

    /** README 里逐条要核的主张对应的表字段名（用于定位行，不用于定位数字）。 */
    private static final String F_VERSION = "当前版本";
    private static final String F_PORT = "默认端口";
    private static final String F_PARENT = "父项目";

    // ==================================================================
    // 基础设施
    // ==================================================================

    /**
     * 定位仓根：从 user.dir 向上找<b>聚合根</b>的 pom.xml。
     *
     * <p>判据要同时满足三条：<b>项目自身</b>的 artifactId 命中（用 XML 解析取顶层
     * {@code <artifactId>}，不是全文 contains —— 本仓 {@code bootstrap/pom.xml} 的
     * {@code <parent>} 里就写着 {@code z-ctc}，全文匹配会把它误判成仓根）+
     * {@code <packaging>pom</packaging>} + 至少一个 {@code <module>}。</p>
     */
    private static Path repoRoot() {
        Path p = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 10 && p != null; i++, p = p.getParent()) {
            Path pom = p.resolve(ROOT_POM);
            if (Files.isRegularFile(pom)) {
                String t = stripXmlComments(readText(pom));
                boolean isRoot = ownArtifactId(t).equals(ROOT_ARTIFACT_ID)
                        && t.contains("<packaging>pom</packaging>")
                        && Pattern.compile("<module>").matcher(t).find();
                if (isRoot) {
                    return p;
                }
            }
        }
        throw new IllegalStateException("未找到 z-ctc 仓根：从 " + Paths.get("").toAbsolutePath()
                + " 向上找不到同时满足 artifactId=" + ROOT_ARTIFACT_ID + " / packaging=pom / 带 <module> 的 pom.xml");
    }

    /** 取 pom 里<b>项目自身</b>的 artifactId（顶层元素，不是 parent 里的那个）。 */
    private static String ownArtifactId(String pomText) {
        Document doc = parseXml(pomText);
        Element project = doc.getDocumentElement();
        NodeList kids = project.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && "artifactId".equals(n.getNodeName())) {
                return n.getTextContent().trim();
            }
        }
        return "";
    }

    private static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            return f.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("XML 解析失败", e);
        }
    }

    private static String readText(Path p) {
        try {
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读文件失败：" + p, e);
        }
    }

    /** 剥掉 XML 注释（根 pom 里注释掉的 {@code <module>} 会骗过正则）。 */
    private static String stripXmlComments(String s) {
        return s.replaceAll("(?s)<!--.*?-->", "");
    }

    /**
     * 剥掉 Java 注释（块注释 + 行注释），字符串字面量原样保留。
     *
     * <p>判「类级注解」还是「方法级注解」时必须先剥注释：Javadoc 里出现过
     * {@code @RequestMapping} 字样就会被当成真映射。</p>
     */
    private static String stripComments(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                char quote = c;
                sb.append(c);
                i++;
                while (i < n) {
                    char d = s.charAt(i);
                    sb.append(d);
                    i++;
                    if (d == '\\' && i < n) {
                        sb.append(s.charAt(i));
                        i++;
                        continue;
                    }
                    if (d == quote) {
                        break;
                    }
                }
                continue;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(n, i + 2);
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    // ==================================================================
    // README 侧解析
    // ==================================================================

    private static int headingLevel(String heading) {
        int n = 0;
        while (n < heading.length() && heading.charAt(n) == '#') {
            n++;
        }
        return n;
    }

    /**
     * 去掉行首 #、emoji 与变体选择符，剩下的就是标题文字。
     *
     * <p>emoji 用码点判断（不吃编译编码的亏）：U+1F300–U+1FAFF、U+2600–U+27BF、
     * 变体选择符 U+FE0F、零宽连接符 U+200D。</p>
     */
    private static String headingText(String raw) {
        String t = raw.trim();
        while (t.startsWith("#")) {
            t = t.substring(1);
        }
        t = t.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            int cp = t.codePointAt(i);
            if (cp == 0xFE0F || cp == 0x200D
                    || (cp >= 0x1F300 && cp <= 0x1FAFF)
                    || (cp >= 0x2600 && cp <= 0x27BF)) {
                if (cp > 0xFFFF) {
                    i++;
                }
                continue;
            }
            sb.appendCodePoint(cp);
        }
        return sb.toString().trim();
    }

    /**
     * 取某个标题的小节正文（到下一个「同级或更高级」标题为止，{@code ###} 子标题仍属本节）。
     *
     * <p>围栏代码块内以 # 开头的行（项目结构树）不算标题，必须跳过。</p>
     */
    private static String section(String readme, String headingText) {
        String[] lines = readme.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        boolean inFence = false;
        boolean started = false;
        int level = 0;
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("```") || t.startsWith("~~~")) {
                inFence = !inFence;
                if (started) {
                    sb.append(line).append('\n');
                }
                continue;
            }
            if (!inFence && t.startsWith("#")) {
                int lv = headingLevel(t);
                String text = headingText(t);
                if (started) {
                    if (lv <= level) {
                        break;
                    }
                } else {
                    if (text.equals(headingText)) {
                        started = true;
                        level = lv;
                        continue;
                    }
                }
            }
            if (started) {
                sb.append(line).append('\n');
            }
        }
        if (!started) {
            throw new IllegalStateException("README 里找不到标题小节：" + headingText);
        }
        return sb.toString();
    }

    /** 从「字段 | 值」表格里抠某个字段的值。 */
    private static String tableCell(String section, String field) {
        for (String line : section.split("\n", -1)) {
            String t = line.trim();
            if (!t.startsWith("|")) {
                continue;
            }
            String[] cells = t.split("\\|", -1);
            if (cells.length < 3) {
                continue;
            }
            String k = cells[1].trim();
            if (k.equals("**" + field + "**") || k.equals(field)) {
                return cells[2].trim();
            }
        }
        throw new IllegalStateException("表格里找不到字段：" + field);
    }

    /** 抠出 `` `xxx` `` 形式的反引号片段。 */
    private static List<String> backticked(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("`([^`\n]+)`").matcher(text);
        while (m.find()) {
            out.add(m.group(1).trim());
        }
        return out;
    }

    // ==================================================================
    // 源码侧事实
    // ==================================================================

    /** 事实对象：<b>只装源码侧</b>的事实。README 侧的数字一律现解析。 */
    private static final class Facts {
        String revision = "";
        String parentVersion = "";
        List<String> modules = new ArrayList<>();
        List<String> controllers = new ArrayList<>();
        List<String> apiPrefixes = new ArrayList<>();
        int port = -1;
        String devContextPath = "";
        List<String> ssoKeys = new ArrayList<>();
        List<String> dbEnvVars = new ArrayList<>();
        List<String> k8sFiles = new ArrayList<>();
        List<String> frontendDirs = new ArrayList<>();
        List<String> tokenServiceImpls = new ArrayList<>();
        String mybatisPlusAdminVersion = "";
    }

    private static Facts facts() {
        Path root = repoRoot();
        Facts f = new Facts();

        // 根 pom：<revision> / parent version / <modules>
        String rootPom = stripXmlComments(readText(root.resolve(ROOT_POM)));
        Document doc = parseXml(rootPom);
        Element project = doc.getDocumentElement();
        NodeList kids = project.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String name = n.getNodeName();
            if ("parent".equals(name)) {
                NodeList pk = n.getChildNodes();
                for (int j = 0; j < pk.getLength(); j++) {
                    Node p = pk.item(j);
                    if (p.getNodeType() == Node.ELEMENT_NODE && "version".equals(p.getNodeName())) {
                        f.parentVersion = p.getTextContent().trim();
                    }
                }
            } else if ("properties".equals(name)) {
                NodeList pk = n.getChildNodes();
                for (int j = 0; j < pk.getLength(); j++) {
                    Node p = pk.item(j);
                    if (p.getNodeType() == Node.ELEMENT_NODE && "revision".equals(p.getNodeName())) {
                        f.revision = p.getTextContent().trim();
                    }
                }
            } else if ("modules".equals(name)) {
                NodeList mk = n.getChildNodes();
                for (int j = 0; j < mk.getLength(); j++) {
                    Node m = mk.item(j);
                    if (m.getNodeType() == Node.ELEMENT_NODE && "module".equals(m.getNodeName())) {
                        f.modules.add(m.getTextContent().trim());
                    }
                }
            }
        }

        // Controller 名单 + API 前缀
        Path apiDir = root.resolve(WEB_API_DIR);
        List<Path> controllerFiles;
        try (Stream<Path> s = Files.list(apiDir)) {
            controllerFiles = s.filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (Path p : controllerFiles) {
            String simple = p.getFileName().toString().replace(".java", "");
            f.controllers.add(simple);
            String src = stripComments(readText(p));
            Matcher m = Pattern.compile("@RequestMapping\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"").matcher(src);
            String prefix = null;
            while (m.find()) {
                // 类级注解：出现在 class 声明之前；这里取第一个匹配（类级注解在文件上方）
                prefix = m.group(1);
                break;
            }
            if (prefix != null) {
                f.apiPrefixes.add(prefix);
            }
        }

        // 端口与 dev context-path
        f.port = yamlServerPort(readText(root.resolve(ADMIN_YML)));
        f.devContextPath = yamlContextPath(readText(root.resolve(ADMIN_DEV_YML)));

        // SSO 配置键：SsoProperties 的 private 字段 → kebab-case
        String ssoProps = stripComments(readText(root.resolve(SSO_PROPERTIES)));
        Matcher sm = Pattern.compile("private\\s+(?:final\\s+)?[\\w<>,\\.\\[\\]\\s]+?\\s+(\\w+)\\s*=").matcher(ssoProps);
        while (sm.find()) {
            f.ssoKeys.add(toKebab(sm.group(1)));
        }

        // 数据源环境变量：yml 里 ${DB_xxx:...} 的变量名
        Set<String> env = new LinkedHashSet<>();
        Matcher em = Pattern.compile("\\$\\{(DB_[A-Z_]+):").matcher(readText(root.resolve(ADMIN_YML)));
        while (em.find()) {
            env.add(em.group(1));
        }
        f.dbEnvVars.addAll(env);

        // k8s 清单
        try (Stream<Path> s = Files.list(root.resolve(K8S_DIR))) {
            f.k8sFiles.addAll(s.filter(p -> p.getFileName().toString().endsWith(".yaml"))
                    .map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // 前端工程
        try (Stream<Path> s = Files.list(root.resolve(FRONTEND_DIR))) {
            f.frontendDirs.addAll(s.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // TokenService 实现
        try (Stream<Path> s = Files.walk(root.resolve(SSO_MAIN))) {
            for (Path p : s.filter(x -> x.getFileName().toString().endsWith(".java")).collect(Collectors.toList())) {
                String src = stripComments(readText(p));
                if (src.contains("implements TokenService")) {
                    f.tokenServiceImpls.add(p.getFileName().toString().replace(".java", ""));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // z-ctc-admin 钉的 MyBatis-Plus 版本
        String adminPom = stripXmlComments(readText(root.resolve(ADMIN_POM)));
        Matcher mp = Pattern.compile("<artifactId>mybatis-plus-boot-starter</artifactId>\\s*<version>([^<]+)</version>")
                .matcher(adminPom);
        if (mp.find()) {
            f.mybatisPlusAdminVersion = mp.group(1).trim();
        }

        return f;
    }

    private static String toKebab(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('-').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 解析顶层 {@code server:} 块里的 {@code port:}。 */
    private static int yamlServerPort(String yml) {
        String[] lines = yml.split("\n", -1);
        boolean inServer = false;
        for (String line : lines) {
            if (line.matches("^server:\\s*$")) {
                inServer = true;
                continue;
            }
            if (inServer) {
                if (line.matches("^\\S.*")) {
                    break;
                }
                Matcher m = Pattern.compile("^\\s+port:\\s*(\\d+)\\s*$").matcher(line);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        throw new IllegalStateException("yml 里找不到 server.port");
    }

    /** 解析顶层 {@code server:} 块里 {@code servlet:} 下的 {@code context-path:}。 */
    private static String yamlContextPath(String yml) {
        String[] lines = yml.split("\n", -1);
        boolean inServer = false;
        boolean inServlet = false;
        for (String line : lines) {
            if (line.matches("^server:\\s*$")) {
                inServer = true;
                continue;
            }
            if (!inServer) {
                continue;
            }
            if (line.matches("^\\S.*")) {
                break;
            }
            if (line.matches("^\\s+servlet:\\s*$")) {
                inServlet = true;
                continue;
            }
            if (inServlet) {
                Matcher m = Pattern.compile("^\\s+context-path:\\s*(\\S+)\\s*$").matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
                if (line.matches("^\\s{2}\\S.*")) {
                    inServlet = false;
                }
            }
        }
        throw new IllegalStateException("yml 里找不到 server.servlet.context-path");
    }

    // ==================================================================
    // 核对：README（现解析） ↔ Facts（源码侧）
    // ==================================================================

    private static List<String> verify(String readme, Facts f) {
        List<String> problems = new ArrayList<>();
        Path root = repoRoot();

        // ---- 基本信息表：当前版本 / 父项目 / 默认端口 ----
        String basic = section(readme, "基本信息");
        String readmeVersion = firstBackticked(tableCell(basic, F_VERSION));
        if (!readmeVersion.equals(f.revision)) {
            problems.add("【当前版本】README 写 " + readmeVersion + "，根 POM <revision> 是 " + f.revision);
        }
        String readmeParent = firstBackticked(tableCell(basic, F_PARENT));
        String expectParent = "z-boot-parent:" + f.parentVersion;
        if (!readmeParent.contains(expectParent)) {
            problems.add("【父项目】README 写 " + readmeParent + "，根 POM parent 是 " + expectParent);
        }
        String readmePort = firstBackticked(tableCell(basic, F_PORT));
        if (!readmePort.equals(String.valueOf(f.port))) {
            problems.add("【默认端口】README 写 " + readmePort + "，application.yml 是 " + f.port);
        }
        if (!readmePort.isEmpty() && !basic.contains("context-path 为 `/ctc`")) {
            problems.add("【默认端口】README 声称 dev profile 下 context-path 为 /ctc，"
                    + "但基本信息表里没写这句（实际 application-dev.yml 是 " + f.devContextPath + "）");
        }
        if (!("/ctc".equals(f.devContextPath))) {
            problems.add("【dev context-path】application-dev.yml 是 " + f.devContextPath + "，期望 /ctc");
        }

        // ---- 项目结构树：Maven 模块清单 ----
        String structure = section(readme, "项目结构");
        Set<String> readmeModules = new TreeSet<>();
        Matcher treeM = Pattern.compile("^(?:├──|└──)\\s+(z-ctc-[\\w-]+)/", Pattern.MULTILINE).matcher(structure);
        while (treeM.find()) {
            readmeModules.add(treeM.group(1));
        }
        Set<String> sourceModules = new TreeSet<>(f.modules);
        if (!readmeModules.equals(sourceModules)) {
            problems.add("【Maven 模块】README 结构树列了 " + readmeModules + "，根 POM <modules> 是 " + sourceModules);
        }
        if (structure.contains("bootstrap/") && sourceModules.contains("bootstrap")) {
            problems.add("【Maven 模块】README 说 bootstrap 不在根 POM 的 <modules> 里，但源码里它在");
        }

        // ---- 能力清单：Controller 名单 ----
        String capability = section(readme, "能力清单");
        Set<String> readmeControllers = new TreeSet<>();
        for (String b : backticked(capability)) {
            if (b.endsWith("Controller")) {
                readmeControllers.add(b);
            }
        }
        Set<String> sourceControllers = new TreeSet<>(f.controllers);
        if (!readmeControllers.equals(sourceControllers)) {
            problems.add("【Controller 名单】README 列了 " + readmeControllers + "，web/api 下实际是 " + sourceControllers);
        }
        Matcher countM = Pattern.compile("\\*\\*(\\d+)\\s*个\\*\\*\\s*Controller").matcher(capability);
        if (!countM.find()) {
            problems.add("【Controller 数量】能力清单节里找不到「N 个 Controller」的写法");
        } else {
            int readmeCount = Integer.parseInt(countM.group(1));
            if (readmeCount != sourceControllers.size()) {
                problems.add("【Controller 数量】README 写 " + readmeCount + "，web/api 下实际有 " + sourceControllers.size() + " 个");
            }
        }

        // ---- API 一览：路径前缀 ----
        String api = section(readme, "API 一览");
        Set<String> readmePaths = new TreeSet<>();
        for (String line : api.split("\n", -1)) {
            String t = line.trim();
            if (!t.startsWith("|")) {
                continue;
            }
            String[] cells = t.split("\\|", -1);
            if (cells.length < 3) {
                continue;
            }
            String p = firstBackticked(cells[1]);
            if (p.startsWith("/")) {
                readmePaths.add(p);
            }
        }
        Set<String> sourcePaths = new TreeSet<>(f.apiPrefixes);
        if (!readmePaths.equals(sourcePaths)) {
            problems.add("【API 前缀】README 列了 " + readmePaths + "，源码类级 @RequestMapping 是 " + sourcePaths);
        }

        // ---- SSO 接入：配置键 ----
        String sso = section(readme, "SSO 接入");
        Set<String> readmeSsoKeys = new TreeSet<>();
        for (String line : sso.split("\n", -1)) {
            String t = line.trim();
            if (!t.startsWith("|")) {
                continue;
            }
            String[] cells = t.split("\\|", -1);
            if (cells.length < 3) {
                continue;
            }
            String k = firstBackticked(cells[1]);
            if (k.startsWith("sso.")) {
                readmeSsoKeys.add(k.substring("sso.".length()));
            }
        }
        Set<String> sourceSsoKeys = new TreeSet<>(f.ssoKeys);
        if (!readmeSsoKeys.equals(sourceSsoKeys)) {
            problems.add("【SSO 配置键】README 表里列了 " + readmeSsoKeys + "，SsoProperties 字段是 " + sourceSsoKeys);
        }
        // README 负向说明里点名的两个键，确实必须无消费方
        for (String orphan : Arrays.asList("sso.enabled", "sso.callback-url")) {
            if (sourceSsoKeys.contains(orphan.substring("sso.".length()))) {
                problems.add("【SSO 配置键】README 声称 " + orphan + " 无消费方，但 SsoProperties 里有这个字段");
            }
            if (!sso.contains("`" + orphan + "`")) {
                problems.add("【SSO 配置键】README 没点名 " + orphan + " 这个无消费方的键");
            }
        }
        // 拦截路径默认值：定位 sso.intercept-paths 那一行，要求它写明默认值是 /**
        String interceptLine = "";
        for (String line : sso.split("\n", -1)) {
            String t = line.trim();
            if (t.startsWith("|") && t.contains("`sso.intercept-paths`")) {
                interceptLine = t;
                break;
            }
        }
        if (interceptLine.isEmpty()) {
            problems.add("【SSO 拦截路径】SSO 接入表里找不到 sso.intercept-paths 那一行");
        } else {
            // 从「默认值是 X」抠出 X 来比 —— 不能用 contains 判 `/api/**`：
            // README 那一行里有「（不是 `/api/**`）」的否定表述，contains 会冤枉健康页。
            Matcher defM = Pattern.compile("默认值是\\s*`([^`]+)`").matcher(interceptLine);
            if (!defM.find()) {
                problems.add("【SSO 拦截路径】README 的 sso.intercept-paths 行里没写「默认值是 X」");
            } else {
                String readmeDefault = defM.group(1);
                if (!"/**".equals(readmeDefault)) {
                    problems.add("【SSO 拦截路径】README 写默认值是 " + readmeDefault
                            + "，SsoProperties 的默认值是 /**");
                }
            }
        }

        // ---- 快速开始 / 连真实 MySQL：数据源环境变量 ----
        String quick = section(readme, "快速开始");
        Set<String> readmeEnv = new TreeSet<>();
        for (String line : quick.split("\n", -1)) {
            String t = line.trim();
            if (!t.startsWith("|")) {
                continue;
            }
            String[] cells = t.split("\\|", -1);
            if (cells.length < 3) {
                continue;
            }
            for (String b : backticked(cells[1])) {
                if (b.startsWith("DB_")) {
                    readmeEnv.add(b);
                }
            }
        }
        Set<String> sourceEnv = new TreeSet<>(f.dbEnvVars);
        if (!readmeEnv.equals(sourceEnv)) {
            problems.add("【数据源环境变量】README 表里列了 " + readmeEnv + "，application.yml 实际引用 " + sourceEnv);
        }
        // 全仓**生效配置**里无 CTC_DB_*（yml 注释里保留着历史说明，那是叙述不是配置）
        for (String yml : Arrays.asList(ADMIN_YML, ADMIN_DEV_YML)) {
            for (String line : readText(root.resolve(yml)).split("\n", -1)) {
                String t = line.trim();
                if (t.startsWith("#")) {
                    continue;
                }
                if (t.contains("CTC_DB_")) {
                    problems.add("【数据源环境变量】" + yml + " 的生效配置里仍出现 CTC_DB_：" + t);
                }
            }
        }
        if (!quick.contains("CTC_DB_")) {
            problems.add("【数据源环境变量】README 没点明 CTC_DB_* 这一组全仓无定义");
        }

        // ---- 文档目录：链接必须落地 ----
        String docs = section(readme, "文档目录");
        List<String> links = new ArrayList<>();
        Matcher lm = Pattern.compile("\\]\\(([^)]+)\\)").matcher(docs);
        while (lm.find()) {
            String target = lm.group(1).trim();
            if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("#")) {
                continue;
            }
            links.add(target);
        }
        if (links.isEmpty()) {
            problems.add("【文档目录】节里一个本地链接都没抠到");
        }
        for (String link : links) {
            if (!Files.exists(root.resolve(link))) {
                problems.add("【文档目录】README 里的链接目标在盘上不存在：" + link);
            }
        }
        // 不得声称存在实际没有的目录 —— 只看**正向**列表项（以 `- [` 开头、
        // 后接反引号包住的桶路径），负向说明（「本仓没有 xxx」）不算声称存在
        Matcher bucketM = Pattern.compile("^-\\s+\\[`(_doc/[\\w\\-/]+)`\\]", Pattern.MULTILINE).matcher(docs);
        while (bucketM.find()) {
            String bucket = bucketM.group(1);
            if (!Files.exists(root.resolve(bucket))) {
                problems.add("【文档目录】README 声称有 " + bucket + "，但盘上不存在这个路径");
            }
        }
        // 桶数
        List<String> buckets = new ArrayList<>();
        try (Stream<Path> s = Files.list(root.resolve(DOC_DIR))) {
            s.filter(Files::isDirectory).map(p -> DOC_DIR + "/" + p.getFileName().toString()).sorted()
                    .forEach(buckets::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String b : buckets) {
            if (!docs.contains("`" + b + "/`") && !docs.contains("`" + b + "`")) {
                problems.add("【文档目录】盘上有 " + b + "，README 文档目录节没提到它");
            }
        }

        // ---- k8s 清单数（在「测试与验证」节里 —— 部署清单的说明写在那儿） ----
        String testing = section(readme, "测试与验证");
        Matcher k8sM = Pattern.compile("共\\s*(\\d+)\\s*个").matcher(testing);
        if (!k8sM.find()) {
            problems.add("【k8s 清单】测试与验证节里找不到「共 N 个」的写法");
        } else {
            int readmeK8s = Integer.parseInt(k8sM.group(1));
            if (readmeK8s != f.k8sFiles.size()) {
                problems.add("【k8s 清单】README 写 " + readmeK8s + " 个，deploy/k8s 下实际 " + f.k8sFiles.size() + " 个");
            }
        }
        if (!f.k8sFiles.isEmpty()) {
            String first = f.k8sFiles.get(0);
            String last = f.k8sFiles.get(f.k8sFiles.size() - 1);
            if (!testing.contains(first) || !testing.contains(last)) {
                problems.add("【k8s 清单】README 写的编号范围没覆盖实际首尾文件 " + first + " / " + last);
            }
        }

        String structure2 = section(readme, "项目结构");
        Matcher feM = Pattern.compile("配套前端\\s*(\\d+)\\s*个\\s*npm\\s*工程").matcher(structure2);
        if (!feM.find()) {
            problems.add("【前端工程】项目结构节里找不到「配套前端 N 个 npm 工程」的写法");
        } else {
            int readmeFe = Integer.parseInt(feM.group(1));
            if (readmeFe != f.frontendDirs.size()) {
                problems.add("【前端工程】README 写 " + readmeFe + " 个，_frontend 下实际 " + f.frontendDirs.size() + " 个："
                        + f.frontendDirs);
            }
        }
        for (String d : f.frontendDirs) {
            if (!structure2.contains(d + "/")) {
                problems.add("【前端工程】盘上有 _frontend/" + d + "/，README 结构树里没列");
            }
        }

        // ---- TokenService 三实现（接口本身不算实现） ----
        String capability2 = section(readme, "能力清单");
        Set<String> readmeImpls = new TreeSet<>();
        for (String b : backticked(capability2)) {
            if (b.endsWith("TokenService") && !"TokenService".equals(b)) {
                readmeImpls.add(b);
            }
        }
        Set<String> sourceImpls = new TreeSet<>(f.tokenServiceImpls);
        if (!readmeImpls.equals(sourceImpls)) {
            problems.add("【TokenService 实现】README 列了 " + readmeImpls + "，z-ctc-sso 源码里是 " + sourceImpls);
        }
        Matcher implCount = Pattern.compile("有\\s*\\*\\*(\\d+)\\*\\*\\s*种实现").matcher(capability2);
        if (!implCount.find()) {
            problems.add("【TokenService 实现】能力清单节里找不到「有 N 种实现」的写法");
        } else {
            int readmeImplCount = Integer.parseInt(implCount.group(1));
            if (readmeImplCount != sourceImpls.size()) {
                problems.add("【TokenService 实现】README 写 " + readmeImplCount + " 种，源码里是 " + sourceImpls.size() + " 种");
            }
        }

        // ---- 技术栈：MyBatis-Plus 版本 ----
        String stack = section(readme, "技术栈");
        if (!f.mybatisPlusAdminVersion.isEmpty() && !stack.contains(f.mybatisPlusAdminVersion)) {
            problems.add("【MyBatis-Plus】z-ctc-admin 钉的版本是 " + f.mybatisPlusAdminVersion + "，README 技术栈表里没写这个版本");
        }

        return problems;
    }

    private static String firstBackticked(String s) {
        List<String> b = backticked(s);
        return b.isEmpty() ? "" : b.get(0);
    }

    // ==================================================================
    // 用例
    // ==================================================================

    @Test
    @DisplayName("README 的全部定量主张与源码重算值逐条对账（一次全报）")
    void README主张与源码重算逐条对账() {
        List<String> problems = verify(readme(), facts());
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder("z-ctc README 与源码不一致，共 " + problems.size() + " 处：");
            for (String p : problems) {
                sb.append("\n  - ").append(p);
            }
            throw new AssertionError(sb.toString());
        }
    }

    @Test
    @DisplayName("不许冤枉健康页：真实 README 不得被判红")
    void 不许冤枉健康页() {
        List<String> problems = verify(readme(), facts());
        assertTrue(problems.isEmpty(), "真实 README 被判红：\n" + String.join("\n", problems));
    }

    @Test
    @DisplayName("尺子的牙口：把版本号改错必须报红")
    void 尺子的牙口_版本改错必红() {
        String readme = readme();
        String mutated = readme.replace("`" + facts().revision + "`", "`9.9.9`");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到当前版本字面量");
        List<String> problems = verify(mutated, facts());
        assertFalse(problems.isEmpty(), "README 版本号改成 9.9.9，尺子居然没报红");
        assertTrue(problems.toString().contains("9.9.9"), "报错没指出被改坏的数字：\n" + String.join("\n", problems));
    }

    @Test
    @DisplayName("尺子的牙口：把 Controller 个数改错必须报红")
    void 尺子的牙口_Controller个数改错必红() {
        String readme = readme();
        String mutated = readme.replace("**12 个** Controller", "**11 个** Controller");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到 Controller 个数写法");
        List<String> problems = verify(mutated, facts());
        assertFalse(problems.isEmpty(), "README Controller 个数改成 11，尺子居然没报红");
    }

    @Test
    @DisplayName("尺子的牙口：删掉一个模块目录必须报红")
    void 尺子的牙口_模块清单改错必红() {
        String readme = readme();
        String mutated = readme.replace("├── z-ctc-client/", "├── z-ctc-clienX/");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到 z-ctc-client 目录行");
        List<String> problems = verify(mutated, facts());
        assertFalse(problems.isEmpty(), "README 模块清单被改，尺子居然没报红");
    }

    @Test
    @DisplayName("尺子的牙口：把默认端口改错必须报红")
    void 尺子的牙口_端口改错必红() {
        String readme = readme();
        String mutated = readme.replace("| **默认端口** | `" + facts().port + "`", "| **默认端口** | `8899`");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到默认端口");
        List<String> problems = verify(mutated, facts());
        assertFalse(problems.isEmpty(), "README 默认端口改成 8899，尺子居然没报红");
        assertTrue(problems.toString().contains("8899"), "报错没指出被改坏的端口：\n" + String.join("\n", problems));
    }

    @Test
    @DisplayName("尺子的牙口：把数据源环境变量换成 CTC_DB_* 必须报红")
    void 尺子的牙口_环境变量改错必红() {
        String readme = readme();
        String mutated = readme.replace("`DB_HOST`", "`CTC_DB_HOST`");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到 DB_HOST");
        List<String> problems = verify(mutated, facts());
        assertFalse(problems.isEmpty(), "README 环境变量被换成 CTC_DB_*，尺子居然没报红");
    }

    @Test
    @DisplayName("尺子的牙口：把 SSO 拦截路径默认值改错必须报红")
    void 尺子的牙口_SSO默认值改错必红() {
        String readme = readme();
        String mutated = readme.replace("的默认值是 `/**`", "的默认值是 `/api/**`");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到 intercept-paths 默认值");
        List<String> problems = verify(mutated, facts());
        assertFalse(problems.isEmpty(), "README SSO 拦截路径默认值被改，尺子居然没报红");
    }

    @Test
    @DisplayName("尺子的牙口：文档目录链接指向不存在的文件必须报红")
    void 尺子的牙口_文档链接失效必红() {
        String readme = readme();
        // 假路径**必须拆两段拼接**：源码里若出现连续的「不存在的路径」字面量，
        // depth_check 会判成「悬空字面量」违规（它连注释行一起扫）。
        String brokenLink = "_doc/001_arch/" + "99-not-exist.md";
        String mutated = readme.replace("_doc/001_arch/00-overview.md", brokenLink);
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到 00-overview.md 链接");
        List<String> problems = verify(mutated, facts());
        assertFalse(problems.isEmpty(), "README 文档链接指向不存在的文件，尺子居然没报红");
    }

    @Test
    @DisplayName("尺子的牙口：把 API 前缀改错必须报红")
    void 尺子的牙口_API前缀改错必红() {
        String readme = readme();
        String mutated = readme.replace("`/api/ctc/authn`", "`/api/ctc/authx`");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到 authn 前缀");
        List<String> problems = verify(mutated, facts());
        assertFalse(problems.isEmpty(), "README API 前缀被改，尺子居然没报红");
    }

    @Test
    @DisplayName("守卫生效确认：它真的在读这份 README（改 README 必红）")
    void 守卫生效确认() {
        String readme = readme();
        // 抠不到就红 —— 证明尺子读的是盘上这份 README，而不是某个写死的副本
        assertTrue(readme.contains("z-ctc"), "读到的 README 不含仓名，仓根定位可能错了");
        assertEquals(facts().controllers.size(),
                new TreeSet<>(facts().controllers).size(),
                "Controller 名单里有重复项，源码侧事实收集有问题");
    }

    private static String readme() {
        return readText(repoRoot().resolve("README.md"));
    }

    /** 保留断言导入，避免 IDE 误判未使用。 */
    @Test
    @DisplayName("断言工具自检")
    void 断言工具自检() {
        assertThrows(IllegalStateException.class, () -> section(readme(), "不存在的标题"));
    }
}
