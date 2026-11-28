package com.zifang.ctc.web.api;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.ctc.core.ak.AkCaller;
import com.zifang.ctc.core.ak.AkException;
import com.zifang.ctc.core.ak.AkService;
import com.zifang.ctc.core.ak.AkServiceImpl;
import com.zifang.ctc.core.ak.DefaultAkScopeCatalog;
import com.zifang.ctc.core.ak.entity.AkCallLogDO;
import com.zifang.ctc.core.ak.entity.AkDO;
import com.zifang.ctc.core.ak.entity.AkScopeDO;
import com.zifang.ctc.core.ak.mapper.AkCallLogMapper;
import com.zifang.ctc.core.ak.mapper.AkMapper;
import com.zifang.ctc.core.ak.mapper.AkScopeMapper;
import com.zifang.ctc.core.ak.model.AkCallLogInput;
import com.zifang.ctc.core.ak.model.AkCallLogQuery;
import com.zifang.ctc.core.ak.model.AkIdentity;
import com.zifang.ctc.core.ak.model.AkPayload;
import com.zifang.ctc.core.ak.model.AkRecord;
import com.zifang.ctc.core.ak.model.AkScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AK 台账域安全口径守卫（H2 + 真实 MyBatis Mapper）。
 *
 * <p>AK 真源在认证中心，这几条是硬约束：库内只存哈希、明文只返回一次、
 * 空范围默认拒绝、调用日志不得落原值、跨租户不可见。</p>
 */
class AkServiceIntegrationTest {

    private JdbcTemplate jdbc;
    private AkService service;
    private final AkCaller alice = new AkCaller("tenant-a", "alice", "Alice");
    private final AkCaller bob = new AkCaller("tenant-b", "bob", "Bob");

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:ctc_ak_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        jdbc = new JdbcTemplate(dataSource);
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(
                new ClassPathResource("db/h2/z-ctc-ak.sql"));
        schema.setSqlScriptEncoding("UTF-8");
        schema.execute(dataSource);

        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(AkMapper.class);
        configuration.addMapper(AkScopeMapper.class);
        configuration.addMapper(AkCallLogMapper.class);
        factory.setConfiguration(configuration);
        SqlSessionFactory sqlSessionFactory = factory.getObject();
        SqlSession session = sqlSessionFactory.openSession(true);
        service = new AkServiceImpl(session.getMapper(AkMapper.class),
                session.getMapper(AkScopeMapper.class),
                session.getMapper(AkCallLogMapper.class),
                new DefaultAkScopeCatalog());
    }

    @Test
    void createReturnsPlaintextOnceAndStoresOnlyHash() throws Exception {
        AkRecord created = create("one-shot", alice, scope("KB", "CALL", "*"));
        assertTrue(created.getSecret() != null && created.getSecret().length() >= 32);
        String stored = jdbc.queryForObject(
                "SELECT secret_hash FROM z_ctc_ak WHERE ak_code = ?", String.class, created.getAkCode());
        assertEquals(sha256(created.getSecret()), stored);
        assertNotEquals(created.getSecret(), stored);
        assertFalse(stored.contains(created.getSecret()));
        // 列表里不得再出现明文
        AkRecord listed = service.list(null, alice).get(0);
        assertEquals(null, listed.getSecret());
    }

    @Test
    void duplicateNameUnderSameOwnerIsRejected() {
        create("dup", alice, scope("KB", "CALL", "*"));
        AkException error = assertThrows(AkException.class, () -> create("dup", alice, null));
        assertEquals(AkException.AK_DUPLICATE_NAME, error.getCode());
    }

    @Test
    void listsAreTenantScoped() {
        create("alice-key", alice, scope("KB", "CALL", "*"));
        assertEquals(1, service.list(null, alice).size());
        assertTrue(service.list(null, bob).isEmpty());
        assertTrue(service.adminList(null, bob).isEmpty());
    }

    @Test
    void verifyRejectsWrongSecret() {
        AkRecord created = create("verify-key", alice, scope("KB", "CALL", "*"));
        AkIdentity identity = service.verify(created.getSecret());
        assertEquals("tenant-a", identity.getTenantCode());
        assertEquals("alice", identity.getOwnerId());
        AkException error = assertThrows(AkException.class, () -> service.verify(created.getSecret() + "x"));
        assertEquals(AkException.AK_NOT_FOUND, error.getCode());
    }

    @Test
    void emptyScopeIsDeniedAndScopeMustMatchAllThreeDimensions() {
        AkRecord empty = create("deny-all", alice, null);
        AkIdentity denied = service.verify(empty.getSecret());
        assertFalse(service.authorize(denied, "KB", "CALL", "/api/kb/v1/client/kb/ping"));

        AkRecord scoped = create("scoped", alice, scope("KB", "CALL", "/api/kb/v1/client/kb/ping"));
        AkIdentity allowed = service.verify(scoped.getSecret());
        assertTrue(service.authorize(allowed, "KB", "CALL", "/api/kb/v1/client/kb/ping"));
        assertFalse(service.authorize(allowed, "ONTOLOGY", "CALL", "/api/kb/v1/client/ontology/info"));
        assertFalse(service.authorize(allowed, "KB", "WRITE", "/api/kb/v1/client/kb/ping"));
    }

    @Test
    void callLogKeepsNoPlaintextAndMasksSensitiveHeaders() {
        AkRecord key = create("log-key", alice, scope("KB", "CALL", "*"));
        AkIdentity identity = service.verify(key.getSecret());
        AkCallLogInput input = new AkCallLogInput();
        input.setTraceId("trace-1");
        input.setHttpMethod("POST");
        input.setRequestPath("/api/kb/v1/client/kb/ping");
        input.setQuery("page=1&token=query-secret&echo=" + key.getSecret());
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer header-secret");
        headers.put("X-AK", key.getSecret());
        headers.put("X-Echo", key.getSecret());
        input.setRequestHeaders(headers);
        input.setRequestBody("{\"password\":\"body-secret\",\"echo\":\"" + key.getSecret() + "\"}");
        input.setResponseBody("{\"access_token\":\"response-secret\"}");
        input.setHttpStatus(200);
        input.setOutcome("SUCCESS");
        input.setDurationMs(5);
        service.recordCall(identity, input);

        String raw = jdbc.queryForObject(
                "SELECT CONCAT(query, '|', request_headers_json, '|', request_body, '|', response_body)"
                        + " FROM z_ctc_ak_call_log", String.class);
        for (String sensitive : Arrays.asList(key.getSecret(), "header-secret", "query-secret",
                "body-secret", "response-secret")) {
            assertFalse(raw.contains(sensitive), "调用日志不得含敏感原值: " + sensitive);
        }
        assertFalse(raw.contains("X-AK"), "X-AK 请求头不得落库");
        assertTrue(raw.contains("***"));
        assertEquals(1, service.callLogs(null, alice).getRecords().size());
        assertTrue(service.callLogs(null, bob).getRecords().isEmpty());
        AkCallLogQuery byId = new AkCallLogQuery();
        byId.setAkId(identity.getAkId());
        byId.setMethod("POST");
        byId.setPathKeyword("ping");
        byId.setOutcome("SUCCESS");
        assertEquals(1, service.callLogs(byId, alice).getItems().size());
    }

    @Test
    void scopeTreeExposesOnlyRegisteredDomains() {
        List<String> domains = service.tree(alice).stream()
                .map(domain -> domain.getDomain()).sorted().toList();
        assertEquals(Arrays.asList("KB", "ONTOLOGY", "SYSTEM"), domains);
    }

    @Test
    void entitiesMapToCtcTables() {
        // 实体与表名绑定（改名会静默写到别的表）
        assertEquals("z_ctc_ak", tableName(AkDO.class));
        assertEquals("z_ctc_ak_scope", tableName(AkScopeDO.class));
        assertEquals("z_ctc_ak_call_log", tableName(AkCallLogDO.class));
    }

    private static String tableName(Class<?> type) {
        com.baomidou.mybatisplus.annotation.TableName annotation =
                type.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
        return annotation == null ? null : annotation.value();
    }

    private AkRecord create(String name, AkCaller caller, AkScope scope) {
        AkPayload payload = new AkPayload();
        payload.setOwnerType("PERSONAL");
        payload.setName(name);
        if (scope != null) {
            payload.setScopesByDomain(Collections.singletonMap(scope.getDomain(),
                    Collections.singletonList(scope)));
        }
        return service.create(payload, caller);
    }

    private static AkScope scope(String domain, String action, String resource) {
        return new AkScope(domain, action, resource, null);
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte b : digest) {
            out.append(String.format("%02x", b & 0xff));
        }
        return out.toString();
    }
}
