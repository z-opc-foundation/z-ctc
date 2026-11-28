package com.zifang.ctc.core.ak;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.core.ak.model.AkCallLogDetail;
import com.zifang.ctc.core.ak.model.AkCallLogInput;
import com.zifang.ctc.core.ak.model.AkCallLogPage;
import com.zifang.ctc.core.ak.model.AkCallLogQuery;
import com.zifang.ctc.core.ak.model.AkCallLogRecord;
import com.zifang.ctc.core.ak.model.AkIdentity;
import com.zifang.ctc.core.ak.model.AkScope;
import com.zifang.ctc.core.ak.model.AkScopeDomain;
import com.zifang.ctc.core.ak.model.AkScopeSaveRequest;
import com.zifang.ctc.core.ak.model.AkPayload;
import com.zifang.ctc.core.ak.model.AkRecord;
import com.zifang.ctc.core.ak.AkService;
import com.zifang.ctc.core.ak.AkException;
import com.zifang.ctc.core.ak.AkCaller;
import com.zifang.ctc.core.ak.AkCallPayloadSanitizer;
import com.zifang.ctc.core.ak.DefaultAkScopeCatalog;
import com.zifang.ctc.core.ak.AkScopeCatalog;
import com.zifang.ctc.core.ak.entity.AkCallLogDO;
import com.zifang.ctc.core.ak.entity.AkScopeDO;
import com.zifang.ctc.core.ak.mapper.AkCallLogMapper;
import com.zifang.ctc.core.ak.mapper.AkScopeMapper;
import com.zifang.ctc.core.ak.entity.AkDO;
import com.zifang.ctc.core.ak.mapper.AkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** AK 台账实现（z-ctc 拥有）：secret 只参与创建响应和验签，不落日志。 */
@Service
public class AkServiceImpl implements AkService {
    private static final String OWNER_PERSONAL = "PERSONAL";
    private static final String OWNER_APPLICATION = "APPLICATION";
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final byte[] DUMMY_HASH = sha256Bytes("invalid-ak-dummy");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> HEADERS_TYPE =
            new TypeReference<Map<String, String>>() { };

    private final AkMapper akMapper;
    private AkScopeMapper scopeMapper;
    private AkCallLogMapper callLogMapper;
    private AkScopeCatalog scopeCatalog = new DefaultAkScopeCatalog();

    /** 只装台账（scope 与调用日志后挂）。 */
    public AkServiceImpl(AkMapper akMapper) {
        this.akMapper = akMapper;
    }

    /** 独立测试/显式装配入口。 */
    @Autowired
    public AkServiceImpl(AkMapper akMapper, AkScopeMapper scopeMapper,
                            AkCallLogMapper callLogMapper, AkScopeCatalog scopeCatalog) {
        this(akMapper);
        this.scopeMapper = scopeMapper;
        this.callLogMapper = callLogMapper;
        this.scopeCatalog = scopeCatalog;
    }

    /** 自动装配只补依赖，不替换既有 Bean。 */
    public void attachAccess(AkScopeMapper scopeMapper, AkCallLogMapper callLogMapper,
                             AkScopeCatalog scopeCatalog) {
        this.scopeMapper = scopeMapper;
        this.callLogMapper = callLogMapper;
        this.scopeCatalog = scopeCatalog;
    }

    @Override
    public List<AkRecord> list(AkPayload params, AkCaller caller) {
        requireCaller(caller);
        QueryWrapper<AkDO> wrapper = new QueryWrapper<AkDO>()
                .eq("tenant_code", caller.getTenantCode()).eq("is_deleted", 0)
                .eq("owner_id", caller.getActorId()).orderByDesc("create_time");
        if (params != null) {
            if (hasText(params.getKeyword())) wrapper.like("ak_name", params.getKeyword().trim());
            if (hasText(params.getOwnerTypeFilter())) {
                wrapper.eq("owner_type", normalizeFilter(params.getOwnerTypeFilter()));
            } else if (hasText(params.getOwnerType())) {
                wrapper.eq("owner_type", normalizeFilter(params.getOwnerType()));
            }
            if (hasText(params.getOwnerName())) wrapper.like("owner_name", params.getOwnerName().trim());
        }
        List<AkRecord> out = new ArrayList<>();
        for (AkDO row : akMapper.selectList(wrapper)) out.add(toRecord(row));
        return out;
    }

    @Override
    public List<AkRecord> adminList(AkPayload params, AkCaller caller) {
        requireCaller(caller);
        QueryWrapper<AkDO> wrapper = new QueryWrapper<AkDO>()
                .eq("tenant_code", caller.getTenantCode()).eq("is_deleted", 0)
                .orderByDesc("create_time");
        if (params != null) {
            if (hasText(params.getKeyword())) wrapper.like("ak_name", params.getKeyword().trim());
            if (hasText(params.getOwnerTypeFilter())) {
                wrapper.eq("owner_type", normalizeFilter(params.getOwnerTypeFilter()));
            } else if (hasText(params.getOwnerType())) {
                wrapper.eq("owner_type", normalizeFilter(params.getOwnerType()));
            }
            if (hasText(params.getOwnerName())) wrapper.like("owner_name", params.getOwnerName().trim());
        }
        List<AkRecord> out = new ArrayList<>();
        for (AkDO row : akMapper.selectList(wrapper)) out.add(toRecord(row));
        return out;
    }

    @Override
    public List<AkRecord> clientList(AkPayload params, AkCaller caller) {
        return adminList(params, caller);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AkRecord create(AkPayload payload, AkCaller caller) {
        requireCaller(caller);
        if (payload == null || !hasText(payload.getOwnerType())) {
            throw invalid("归属类型必填");
        }
        String ownerType = payload.getOwnerType().trim().toUpperCase(Locale.ROOT);
        if (!OWNER_PERSONAL.equals(ownerType) && !OWNER_APPLICATION.equals(ownerType)) {
            throw invalid("归属类型只支持 PERSONAL/APPLICATION");
        }
        if (!hasText(payload.getName()) || payload.getName().trim().length() > 128) {
            throw invalid("AK 名称必填且最长 128 字");
        }
        String name = payload.getName().trim();
        Long dup = akMapper.selectCount(new QueryWrapper<AkDO>()
                .eq("tenant_code", caller.getTenantCode()).eq("owner_id", caller.getActorId())
                .eq("ak_name", name).eq("is_deleted", 0));
        if (dup != null && dup > 0) throw new AkException(
                AkException.AK_DUPLICATE_NAME, "同归属下 AK 名称重复: " + name);
        List<AkScope> scopes = normalizeScopes(payload.getScopesByDomain(), false);
        String code = "AK" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase(Locale.ROOT);
        String secret = randomSecret();
        Instant now = Instant.now();
        AkDO entity = new AkDO();
        entity.setId(UUID.randomUUID().toString());
        entity.setTenantCode(caller.getTenantCode());
        entity.setAkCode(code);
        entity.setAkName(name);
        entity.setOwnerType(ownerType);
        entity.setOwnerId(caller.getActorId());
        entity.setOwnerName(caller.getActorName());
        entity.setSecretHash(sha256Hex(secret));
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreateBy(caller.getActorId());
        entity.setUpdateBy(caller.getActorId());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        akMapper.insert(entity);
        replaceScopes(entity, scopes);
        AkRecord record = toRecord(entity);
        record.setSecret(secret);
        return record;
    }

    @Override
    public AkIdentity verify(String secret) {
        if (!hasText(secret) || secret.length() > 256) throw invalid("AK 不能为空");
        byte[] candidate = sha256Bytes(secret.trim());
        // secret 从不以明文入查询条件；遍历有效台账并逐行常量时间比较 SHA-256。
        List<AkDO> rows = akMapper.selectList(new QueryWrapper<AkDO>().eq("is_deleted", 0));
        AkDO matched = null;
        byte[] expected = DUMMY_HASH;
        // 对所有同码行都比较，避免先命中即返回；hash 定长，使用常量时间比较。
        for (AkDO row : rows) {
            expected = hashBytes(row.getSecretHash());
            boolean matches = MessageDigest.isEqual(expected, candidate);
            if (matches && matched == null && Integer.valueOf(1).equals(row.getStatus())) matched = row;
        }
        if (matched == null) throw new AkException(AkException.AK_NOT_FOUND, "AK 无效");
        AkIdentity identity = new AkIdentity();
        identity.setAkId(matched.getId());
        identity.setAkCode(matched.getAkCode());
        identity.setAkMasked(mask(secret.trim()));
        identity.setTenantCode(matched.getTenantCode());
        identity.setAkName(matched.getAkName());
        identity.setOwnerType(matched.getOwnerType());
        identity.setOwnerId(matched.getOwnerId());
        identity.setOwnerName(matched.getOwnerName());
        identity.setEnabled(true);
        return identity;
    }

    @Override
    public boolean authorize(AkIdentity identity, String domain, String action, String resource) {
        if (identity == null || !identity.isEnabled() || scopeMapper == null) return false;
        AkScope requested;
        try {
            requested = AkCallPayloadSanitizer.normalize(new AkScope(domain, action, resource, null));
        } catch (IllegalArgumentException ex) {
            return false;
        }
        List<AkScopeDO> rows = scopeMapper.selectList(new QueryWrapper<AkScopeDO>()
                .eq("tenant_code", identity.getTenantCode()).eq("ak_code", identity.getAkCode())
                .eq("domain", requested.getDomain()).eq("action", requested.getAction()));
        if (rows.isEmpty()) return false; // 空 scope 默认拒绝，绝不 unrestricted。
        for (AkScopeDO row : rows) {
            if ("*".equals(row.getResource()) || requested.getResource().equals(row.getResource())) return true;
        }
        return false;
    }

    @Override
    public List<AkScopeDomain> tree(AkCaller caller) {
        requireCaller(caller);
        return scopeCatalog.tree();
    }

    @Override
    public Map<String, List<AkScope>> detail(String akCode, AkCaller caller, boolean admin) {
        AkDO bound = requireAk(akCode, caller, admin);
        Map<String, List<AkScope>> result = new LinkedHashMap<>();
        for (AkScopeDO row : listScopes(bound)) {
            result.computeIfAbsent(row.getDomain(), key -> new ArrayList<>()).add(toScope(row));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(AkScopeSaveRequest request, AkCaller caller) {
        requireCaller(caller);
        if (request == null || !hasText(request.getAkCode())) throw invalid("akCode 必填");
        AkDO bound = requireAk(request.getAkCode(), caller, true);
        replaceScopes(bound, normalizeScopes(request.getScopesByDomain(), true));
    }

    @Override
    public AkCallLogPage callLogs(AkCallLogQuery query, AkCaller caller) {
        requireCaller(caller);
        requireStorage();
        AkCallLogQuery q = query == null ? new AkCallLogQuery() : query;
        int page = Math.max(1, q.getPage());
        if (hasText(q.getCursor())) {
            try {
                page = Math.max(1, Integer.parseInt(q.getCursor()));
            } catch (NumberFormatException ex) {
                throw invalid("cursor 非法");
            }
        }
        int size = Math.max(1, Math.min(100, q.getSize()));
        Instant to = q.getTo() == null
                ? (q.getEndTime() == null ? Instant.now() : Instant.ofEpochMilli(q.getEndTime())) : q.getTo();
        Instant earliest = to.minus(14, ChronoUnit.DAYS);
        Instant from = q.getFrom() != null ? q.getFrom()
                : (q.getStartTime() == null ? to.minus(1, ChronoUnit.DAYS) : Instant.ofEpochMilli(q.getStartTime()));
        if (from.isBefore(earliest)) from = earliest;
        if (!from.isBefore(to)) throw invalid("时间范围非法");
        QueryWrapper<AkCallLogDO> wrapper = new QueryWrapper<AkCallLogDO>()
                .eq("tenant_code", caller.getTenantCode()).ge("completed_at", from).le("completed_at", to)
                .orderByDesc("completed_at", "id");
        if (hasText(q.getAkCode())) wrapper.eq("ak_code", q.getAkCode().trim());
        if (hasText(q.getAkId())) wrapper.eq("ak_id", q.getAkId().trim());
        if (hasText(q.getMethod())) wrapper.eq("http_method", q.getMethod().trim().toUpperCase(Locale.ROOT));
        if (hasText(q.getPathKeyword())) wrapper.like("request_path", q.getPathKeyword().trim());
        if (hasText(q.getOutcome())) wrapper.eq("outcome", q.getOutcome().trim().toUpperCase(Locale.ROOT));
        List<AkCallLogDO> rows = callLogMapper.selectList(wrapper.last("LIMIT " + size + " OFFSET " + ((page - 1) * size)));
        List<AkCallLogRecord> records = new ArrayList<>();
        for (AkCallLogDO row : rows) records.add(toRecord(row));
        return new AkCallLogPage(records, page, size, rows.size() == size,
                from.toEpochMilli(), to.toEpochMilli());
    }

    @Override
    public AkCallLogDetail callLogDetail(String akCode, String recordId, AkCaller caller) {
        requireCaller(caller);
        requireStorage();
        if (!hasText(akCode) || !hasText(recordId)) throw invalid("akCode/recordId 必填");
        AkCallLogDO row = callLogMapper.selectOne(new QueryWrapper<AkCallLogDO>()
                .eq("tenant_code", caller.getTenantCode()).eq("ak_code", akCode.trim()).eq("id", recordId.trim()));
        if (row == null) throw new AkException(AkException.AK_NOT_FOUND, "调用记录不存在");
        return toCallLogDetail(row);
    }

    @Override
    public AkCallLogDetail callLogDetailByAkId(String akId, String recordId, AkCaller caller) {
        requireCaller(caller);
        requireStorage();
        if (!hasText(akId) || !hasText(recordId)) throw invalid("akId/recordId 必填");
        AkCallLogDO row = callLogMapper.selectOne(new QueryWrapper<AkCallLogDO>()
                .eq("tenant_code", caller.getTenantCode()).eq("ak_id", akId.trim()).eq("id", recordId.trim()));
        if (row == null) throw new AkException(AkException.AK_NOT_FOUND, "调用记录不存在");
        return toCallLogDetail(row);
    }

    private AkCallLogDetail toCallLogDetail(AkCallLogDO row) {
        AkCallLogDetail detail = new AkCallLogDetail();
        copyRecord(row, detail);
        detail.setRequestHeaders(readHeaders(row.getRequestHeadersJson()));
        detail.setRequestBody(AkCallPayloadSanitizer.body(row.getRequestBody()));
        detail.setResponseBody(AkCallPayloadSanitizer.body(row.getResponseBody()));
        detail.setSummary(toRecord(row));
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("requestHeaders", JSON.valueToTree(detail.getRequestHeaders()).toString());
        payload.put("requestBody", detail.getRequestBody());
        payload.put("responseBody", detail.getResponseBody());
        detail.setPayload(payload);
        return detail;
    }

    @Override
    public void recordCall(AkIdentity identity, AkCallLogInput input) {
        if (identity == null || input == null || callLogMapper == null) return;
        Instant now = Instant.now();
        AkCallLogDO row = new AkCallLogDO();
        row.setId(UUID.randomUUID().toString());
        row.setTenantCode(identity.getTenantCode());
        row.setAkId(identity.getAkId());
        row.setAkCode(identity.getAkCode());
        row.setAkMasked(identity.getAkMasked());
        String secret = credential(input);
        row.setAkName(redact(identity.getAkName(), secret));
        row.setOwnerId(redact(identity.getOwnerId(), secret));
        row.setOwnerName(AkCallPayloadSanitizer.safeText(redact(identity.getOwnerName(), secret)));
        row.setTraceId(AkCallPayloadSanitizer.safeText(redact(input.getTraceId(), secret)));
        row.setStartedAt(now.minusMillis(Math.max(0, input.getDurationMs())));
        row.setCompletedAt(now);
        row.setDurationMs(Math.max(0, input.getDurationMs()));
        row.setHttpMethod(AkCallPayloadSanitizer.safeText(redact(input.getHttpMethod(), secret)).toUpperCase(Locale.ROOT));
        row.setRequestPath(AkCallPayloadSanitizer.safeText(redact(input.getRequestPath(), secret)));
        row.setQuery(AkCallPayloadSanitizer.query(redact(input.getQuery(), secret)));
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            if (input.getRequestHeaders() != null) {
                for (Map.Entry<String, String> header : input.getRequestHeaders().entrySet()) {
                    headers.put(header.getKey(), redact(header.getValue(), secret));
                }
            }
            row.setRequestHeadersJson(JSON.writeValueAsString(AkCallPayloadSanitizer.headers(headers)));
        } catch (Exception ex) {
            row.setRequestHeadersJson("{}");
        }
        row.setRequestBody(AkCallPayloadSanitizer.body(redact(input.getRequestBody(), secret)));
        row.setResponseBody(AkCallPayloadSanitizer.body(redact(input.getResponseBody(), secret)));
        row.setHttpStatus(input.getHttpStatus());
        row.setOutcome(AkCallPayloadSanitizer.safeText(redact(input.getOutcome(), secret)));
        row.setErrorSummary(AkCallPayloadSanitizer.safeText(redact(input.getErrorSummary(), secret)));
        callLogMapper.insert(row);
    }

    private static String credential(AkCallLogInput input) {
        if (input.getRequestHeaders() == null) return null;
        for (Map.Entry<String, String> header : input.getRequestHeaders().entrySet()) {
            String name = header.getKey() == null ? "" : header.getKey();
            if ("X-AK".equalsIgnoreCase(name) || "X-FB-AK".equalsIgnoreCase(name)) {
                return header.getValue();
            }
        }
        return null;
    }

    private static String redact(String value, String secret) {
        return AkCallPayloadSanitizer.redactExact(value, secret);
    }

    private List<AkScope> normalizeScopes(Map<String, List<AkScope>> source, boolean requireNonEmpty) {
        if (source == null) source = new LinkedHashMap<>();
        List<AkScope> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map.Entry<String, List<AkScope>> entry : source.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) throw invalid("scope 分组不能为空");
            for (AkScope item : entry.getValue()) {
                final AkScope normalized;
                try {
                    normalized = AkCallPayloadSanitizer.normalize(item);
                } catch (IllegalArgumentException ex) {
                    throw invalid(ex.getMessage());
                }
                AkScope scope = normalized;
                try {
                    scopeCatalog.validate(scope);
                } catch (IllegalArgumentException ex) {
                    throw invalid(ex.getMessage());
                }
                if (!hasText(entry.getKey())
                        || !scope.getDomain().equals(entry.getKey().trim().toUpperCase(Locale.ROOT))) {
                    throw invalid("scope 分组与 domain 不一致");
                }
                String key = scope.getDomain() + "|" + scope.getAction() + "|" + scope.getResource();
                if (seen.add(key)) result.add(scope);
            }
        }
        if (requireNonEmpty && result.isEmpty()) throw invalid("scope 不能为空");
        return result;
    }

    private void replaceScopes(AkDO ak, List<AkScope> scopes) {
        if (scopeMapper == null) {
            if (!scopes.isEmpty()) throw invalid("scope 存储未启用");
            return;
        }
        scopeMapper.delete(new QueryWrapper<AkScopeDO>()
                .eq("tenant_code", ak.getTenantCode()).eq("ak_code", ak.getAkCode()));
        for (AkScope scope : scopes) {
            AkScopeDO row = new AkScopeDO();
            row.setId(UUID.randomUUID().toString());
            row.setTenantCode(ak.getTenantCode());
            row.setAkCode(ak.getAkCode());
            row.setDomain(scope.getDomain());
            row.setAction(scope.getAction());
            row.setResource(scope.getResource());
            row.setResourceName(scope.getName());
            row.setCreateTime(Instant.now());
            scopeMapper.insert(row);
        }
    }

    private AkDO requireAk(String akCode, AkCaller caller, boolean admin) {
        requireCaller(caller);
        if (!hasText(akCode)) throw invalid("akCode 必填");
        List<AkDO> rows = akMapper.selectList(new QueryWrapper<AkDO>()
                .eq("tenant_code", caller.getTenantCode()).eq("ak_code", akCode.trim()).eq("is_deleted", 0));
        if (rows.isEmpty()) throw new AkException(AkException.AK_NOT_FOUND, "AK 不存在");
        AkDO row = rows.get(0);
        if (!admin && !caller.isSameActor(row.getOwnerId())) {
            throw new AkException(AkException.AK_NOT_FOUND, "AK 不存在");
        }
        return row;
    }

    private AkRecord toRecord(AkDO entity) {
        AkRecord record = new AkRecord();
        record.setAkId(entity.getId());
        record.setTenantCode(entity.getTenantCode());
        record.setAkCode(entity.getAkCode());
        record.setAkName(entity.getAkName());
        record.setOwnerType(entity.getOwnerType());
        record.setOwnerId(entity.getOwnerId());
        record.setOwnerName(entity.getOwnerName());
        record.setCreateTime(entity.getCreateTime());
        record.setStatus(entity.getStatus());
        Map<String, List<AkScope>> scopes = new LinkedHashMap<>();
        if (scopeMapper != null) {
            for (AkScopeDO row : listScopes(entity)) {
                scopes.computeIfAbsent(row.getDomain(), key -> new ArrayList<>()).add(toScope(row));
            }
        }
        record.setScopes(scopes);
        return record;
    }

    private List<AkScopeDO> listScopes(AkDO ak) {
        return scopeMapper.selectList(new QueryWrapper<AkScopeDO>()
                .eq("tenant_code", ak.getTenantCode()).eq("ak_code", ak.getAkCode())
                .orderByAsc("domain", "action", "resource"));
    }

    private AkScope toScope(AkScopeDO row) {
        return new AkScope(row.getDomain(), row.getAction(), row.getResource(), row.getResourceName());
    }

    private AkCallLogRecord toRecord(AkCallLogDO row) {
        AkCallLogRecord record = new AkCallLogRecord();
        copyRecord(row, record);
        return record;
    }

    private void copyRecord(AkCallLogDO row, AkCallLogRecord record) {
        record.setRecordId(row.getId());
        record.setTraceId(row.getTraceId());
        record.setAkId(row.getAkId());
        record.setAkCode(row.getAkCode());
        record.setAkMasked(row.getAkMasked());
        record.setAkName(row.getAkName());
        record.setOwnerId(row.getOwnerId());
        record.setOwnerName(row.getOwnerName());
        record.setStartedAt(row.getStartedAt());
        record.setCompletedAt(row.getCompletedAt());
        record.setDurationMs(row.getDurationMs() == null ? 0 : row.getDurationMs());
        record.setHttpMethod(row.getHttpMethod());
        record.setRequestPath(row.getRequestPath());
        record.setQuery(row.getQuery());
        record.setHttpStatus(row.getHttpStatus() == null ? 0 : row.getHttpStatus());
        record.setOutcome(row.getOutcome());
        record.setErrorSummary(row.getErrorSummary());
    }

    private Map<String, String> readHeaders(String value) {
        if (!hasText(value)) return new LinkedHashMap<>();
        try {
            Map<String, String> parsed = JSON.readValue(value, HEADERS_TYPE);
            return AkCallPayloadSanitizer.headers(parsed);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private void requireStorage() {
        if (scopeMapper == null || callLogMapper == null) throw invalid("AK access 存储未启用");
    }

    private static void requireCaller(AkCaller caller) {
        if (caller == null || !hasText(caller.getTenantCode()) || !hasText(caller.getActorId())
                || "anonymous".equals(caller.getActorId())) throw invalid("可信调用人必填");
    }

    private static AkException invalid(String message) {
        return new AkException(AkException.INVALID_PARAM, message);
    }

    private static String normalizeFilter(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        char[] out = new char[64];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            out[i * 2] = HEX[b >>> 4];
            out[i * 2 + 1] = HEX[b & 0x0F];
        }
        return new String(out);
    }

    private static String sha256Hex(String value) {
        return new String(hex(sha256Bytes(value)));
    }

    private static byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static byte[] hashBytes(String hash) {
        if (hash == null || !hash.matches("[0-9a-fA-F]{64}")) return DUMMY_HASH;
        byte[] out = new byte[32];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(hash.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    private static char[] hex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            out[i * 2] = HEX[b >>> 4];
            out[i * 2 + 1] = HEX[b & 0x0F];
        }
        return out;
    }

    private static String mask(String secret) {
        return secret.length() <= 12 ? "***" : secret.substring(0, 6) + "***" + secret.substring(secret.length() - 4);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
