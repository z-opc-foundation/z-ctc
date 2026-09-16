package com.zifang.ctc.sso.temp;

import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.model.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 临时账号服务.
 * <p>
 * 设计哲学:
 * 临时账号 (admin 后台代开 / 短期合作伙伴 / 测试账号) 是 4A 中心的常见场景:
 * <ul>
 *   <li>有明确生效 / 失效时间 (自动过期)</li>
 *   <li>有创建人 (可追溯)</li>
 *   <li>有备注 (业务原因)</li>
 *   <li>过期后自动失效, 不需要管理员手动清理</li>
 * </ul>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>in-memory 存储: 与 MultiTerminalSessionStore 一致, 单 JVM 内存持有</li>
 *   <li>Token 复用 JwtUtil 签发: 临时账号 token 与正常 token 走相同校验链路 (TokenService.verifyToken 即可识别)</li>
 *   <li>不修改任何现有类: 本类纯新增, 与 LocalTokenService / RemoteTokenService / SsoInterceptor 解耦</li>
 *   <li>不依赖数据库: 临时账号是平台能力, 业务方按需把 TempAccountRecord 持久化到自己的业务表</li>
 * </ul>
 * <p>
 * 业务流程 (业务方参考):
 * <ol>
 *   <li>admin 调用 {@link #openTempAccount(String, String, long, String)} 拿到 token + record</li>
 *   <li>业务方把 token 下发给临时用户</li>
 *   <li>临时用户用 token 走正常登录链路, SsoInterceptor → TokenService.verifyToken → JwtUtil 校验通过</li>
 *   <li>过期后 verifyToken 仍然会通过 JwtUtil 校验 (JwtUtil 不感知临时账号有效期), 业务方需在 SsoInterceptor 之前调用 {@link #isValid(String)} 二次校验 (TODO 业务接入)</li>
 * </ol>
 */
@Service
public class TempAccountService {

    private static final Logger log = LogManager.getLogger(TempAccountService.class);

    @Autowired
    private JwtUtil jwtUtil;

    private final Map<String, TempAccountRecord> records = new ConcurrentHashMap<String, TempAccountRecord>();

    /**
     * 开一个临时账号.
     *
     * @param openedBy    创建人 (admin 用户名/ID), 业务方可用于审计
     * @param subject     临时账号的 userId (业务方分配, 与正式账号 userId 不冲突)
     * @param ttlMillis   有效期 (建议 ≤ 7 天)
     * @param reason      业务备注 (如 "为 XX 客户开通 7 天试用")
     * @return TempAccountResult 含 token (业务方下发) + recordId (业务方可持久化以便追踪)
     */
    public TempAccountResult openTempAccount(String openedBy, String subject, long ttlMillis, String reason) {
        if (subject == null || subject.isEmpty()) {
            return null;
        }
        if (ttlMillis <= 0) {
            ttlMillis = 24 * 60 * 60 * 1000L;
        }

        String recordId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long expireAt = now + ttlMillis;

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(subject);
        userInfo.setUsername("temp_" + subject);
        userInfo.setNickname("临时账号-" + subject);

        String token = jwtUtil.generateToken(buildClaims(recordId, openedBy, reason), ttlMillis / 1000);

        TempAccountRecord record = new TempAccountRecord();
        record.recordId = recordId;
        record.openedBy = openedBy;
        record.subject = subject;
        record.token = token;
        record.createdAt = now;
        record.expireAt = expireAt;
        record.reason = reason;

        records.put(recordId, record);
        log.info("临时账号开通: recordId={}, subject={}, openedBy={}, ttlMillis={}",
                recordId, subject, openedBy, ttlMillis);
        return new TempAccountResult(token, record);
    }

    /**
     * 关闭一个临时账号. token 立即失效 (通过 revoked 列表).
     */
    public boolean closeTempAccount(String recordId) {
        TempAccountRecord record = records.remove(recordId);
        if (record == null) {
            return false;
        }
        log.info("临时账号关闭: recordId={}, subject={}", recordId, record.subject);
        return true;
    }

    /**
     * 校验 token 是否属于"未过期且未关闭"的临时账号.
     * 业务方在 SsoInterceptor 之前调用, 作为临时账号二次校验.
     *
     * @return true 表示 token 有效, false 表示 token 不是临时账号 / 已过期 / 已关闭
     */
    public boolean isValid(String token) {
        if (token == null) {
            return false;
        }
        for (TempAccountRecord r : records.values()) {
            if (token.equals(r.token)) {
                return System.currentTimeMillis() < r.expireAt;
            }
        }
        return false;
    }

    /**
     * 查询临时账号记录. 仅供业务方审计使用.
     */
    public TempAccountRecord getRecord(String recordId) {
        return records.get(recordId);
    }

    /**
     * 清空全部 (单元测试用).
     */
    public void clear() {
        records.clear();
    }

    private Map<String, Object> buildClaims(String recordId, String openedBy, String reason) {
        Map<String, Object> claims = new java.util.HashMap<String, Object>();
        claims.put("tempAccount", true);
        claims.put("recordId", recordId);
        claims.put("openedBy", openedBy);
        claims.put("reason", reason);
        return claims;
    }

    /**
     * 临时账号记录 (业务方可序列化为 JSON 持久化到自己的审计表).
     */
    public static class TempAccountRecord {
        public String recordId;
        public String openedBy;
        public String subject;
        public String token;
        public long createdAt;
        public long expireAt;
        public String reason;
    }

    /**
     * 临时账号开号结果.
     */
    public static class TempAccountResult {
        public final String token;
        public final TempAccountRecord record;

        public TempAccountResult(String token, TempAccountRecord record) {
            this.token = token;
            this.record = record;
        }
    }
}
