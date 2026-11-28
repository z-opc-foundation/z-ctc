package com.zifang.ctc.web.api;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.ctc.core.service.session.ServiceSession;
import com.zifang.ctc.core.service.session.ServiceSessionDO;
import com.zifang.ctc.core.service.session.ServiceSessionMapper;
import com.zifang.ctc.core.service.session.ServiceSessionService;
import com.zifang.ctc.core.service.session.ServiceSessionServiceImpl;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 机器 / 服务账号会话守卫（H2 + 真实 MyBatis Mapper）。
 *
 * <p>硬约束：库里只存令牌哈希、明文只返回一次、过期 / 吊销即失效。</p>
 */
class ServiceSessionIntegrationTest {

    private JdbcTemplate jdbc;
    private ServiceSessionService service;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:ctc_svc_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        jdbc = new JdbcTemplate(dataSource);
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(
                new ClassPathResource("db/h2/z-ctc-service-session.sql"));
        schema.setSqlScriptEncoding("UTF-8");
        schema.execute(dataSource);

        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(ServiceSessionMapper.class);
        factory.setConfiguration(configuration);
        SqlSessionFactory sqlSessionFactory = factory.getObject();
        SqlSession session = sqlSessionFactory.openSession(true);
        service = new ServiceSessionServiceImpl(session.getMapper(ServiceSessionMapper.class));
    }

    @Test
    void issueReturnsPlaintextOnceAndStoresOnlyHash() throws Exception {
        ServiceSession issued = service.issue("tenant-a", "DEVICE", "fb-bios-01", 60_000L);
        assertNotNull(issued.getToken());
        String stored = jdbc.queryForObject(
                "SELECT token_hash FROM z_ctc_service_session WHERE id = ?", String.class, issued.getId());
        assertEquals(sha256(issued.getToken()), stored);
        assertNotEquals(issued.getToken(), stored);
        assertFalse(stored.contains(issued.getToken()));
    }

    @Test
    void verifyReturnsIdentityAndUnknownTokenFails() {
        ServiceSession issued = service.issue("tenant-a", "DEVICE", "fb-bios-01", 60_000L);
        ServiceSession verified = service.verify(issued.getToken());
        assertNotNull(verified);
        assertEquals("fb-bios-01", verified.getSubjectCode());
        assertEquals("DEVICE", verified.getSubjectType());
        assertEquals("tenant-a", verified.getTenantCode());
        assertNull(verified.getToken(), "验签结果不得回显令牌");
        assertNull(service.verify("not-a-token"));
        assertNull(service.verify(null));
    }

    @Test
    void revokedAndExpiredTokensAreRejected() {
        ServiceSession issued = service.issue("tenant-a", "DEVICE", "fb-bios-01", 60_000L);
        assertTrue(service.revoke(issued.getToken()));
        assertNull(service.verify(issued.getToken()));
        assertFalse(service.revoke(issued.getToken()));

        ServiceSession expired = service.issue("tenant-a", "DEVICE", "fb-bios-02", 60_000L);
        jdbc.update("UPDATE z_ctc_service_session SET expires_at = 1 WHERE id = ?", expired.getId());
        assertNull(service.verify(expired.getToken()));
    }

    @Test
    void activeCountOnlyCountsLiveSessionsOfTenant() {
        service.issue("tenant-a", "DEVICE", "a-1", 60_000L);
        service.issue("tenant-a", "DEVICE", "a-2", 60_000L);
        service.issue("tenant-b", "DEVICE", "b-1", 60_000L);
        assertEquals(2, service.activeCount("tenant-a"));
        assertEquals(1, service.activeCount("tenant-b"));
    }

    @Test
    void entityMapsToServiceSessionTable() {
        com.baomidou.mybatisplus.annotation.TableName annotation =
                ServiceSessionDO.class.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
        assertEquals("z_ctc_service_session", annotation.value());
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
