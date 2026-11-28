package com.zifang.ctc.core.service.session;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * 机器 / 服务账号会话实现：令牌只存哈希，常量时间比对，验签顺带滑动续期。
 *
 * @author zifang
 */
@Service
public class ServiceSessionServiceImpl implements ServiceSessionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long DEFAULT_TTL_MILLIS = 30 * 60_000L;

    private final ServiceSessionMapper mapper;

    public ServiceSessionServiceImpl(ServiceSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceSession issue(String tenantCode, String subjectType, String subjectCode, long ttlMillis) {
        if (tenantCode == null || tenantCode.trim().isEmpty()) {
            throw new IllegalArgumentException("租户不能为空");
        }
        if (subjectCode == null || subjectCode.trim().isEmpty()) {
            throw new IllegalArgumentException("主体标识不能为空");
        }
        long ttl = ttlMillis > 0 ? ttlMillis : DEFAULT_TTL_MILLIS;
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long now = System.currentTimeMillis();
        ServiceSessionDO entity = new ServiceSessionDO();
        entity.setId(UUID.randomUUID().toString());
        entity.setTenantCode(tenantCode.trim());
        entity.setSubjectType(subjectType == null || subjectType.trim().isEmpty()
                ? "SERVICE" : subjectType.trim().toUpperCase(java.util.Locale.ROOT));
        entity.setSubjectCode(subjectCode.trim());
        entity.setTokenHash(sha256(token));
        entity.setIssuedAt(now);
        entity.setExpiresAt(now + ttl);
        entity.setLastSeenAt(now);
        entity.setRevoked(0);
        entity.setCreateTime(now);
        mapper.insert(entity);
        ServiceSession result = toView(entity);
        result.setToken(token);
        return result;
    }

    @Override
    public ServiceSession verify(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String hash = sha256(token.trim());
        ServiceSessionDO entity = mapper.selectOne(new QueryWrapper<ServiceSessionDO>()
                .eq("token_hash", hash).last("LIMIT 1"));
        if (entity == null || entity.getRevoked() != 0 || entity.getExpiresAt() <= System.currentTimeMillis()) {
            return null;
        }
        long now = System.currentTimeMillis();
        long expiresAt = Math.max(entity.getExpiresAt(), now + DEFAULT_TTL_MILLIS);
        mapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ServiceSessionDO>()
                .eq("token_hash", hash).set("expires_at", expiresAt).set("last_seen_at", now));
        ServiceSession result = toView(entity);
        result.setExpiresAt(expiresAt);
        result.setLastSeenAt(now);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revoke(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        String hash = sha256(token.trim());
        return mapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ServiceSessionDO>()
                .eq("token_hash", hash).eq("revoked", 0).set("revoked", 1)) > 0;
    }

    @Override
    public int activeCount(String tenantCode) {
        QueryWrapper<ServiceSessionDO> wrapper = new QueryWrapper<ServiceSessionDO>()
                .eq("revoked", 0).gt("expires_at", System.currentTimeMillis());
        if (tenantCode != null && !tenantCode.trim().isEmpty()) {
            wrapper.eq("tenant_code", tenantCode.trim());
        }
        Long count = mapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    private static ServiceSession toView(ServiceSessionDO entity) {
        ServiceSession view = new ServiceSession();
        view.setId(entity.getId());
        view.setTenantCode(entity.getTenantCode());
        view.setSubjectType(entity.getSubjectType());
        view.setSubjectCode(entity.getSubjectCode());
        view.setIssuedAt(entity.getIssuedAt());
        view.setExpiresAt(entity.getExpiresAt());
        view.setLastSeenAt(entity.getLastSeenAt());
        return view;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) {
                out.append(String.format("%02x", b & 0xff));
            }
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("令牌哈希失败", e);
        }
    }
}
