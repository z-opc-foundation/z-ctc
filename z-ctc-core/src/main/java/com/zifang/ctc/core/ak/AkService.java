package com.zifang.ctc.core.ak;

import com.zifang.ctc.core.ak.model.AkCallLogDetail;
import com.zifang.ctc.core.ak.model.AkCallLogInput;
import com.zifang.ctc.core.ak.model.AkCallLogPage;
import com.zifang.ctc.core.ak.model.AkCallLogQuery;
import com.zifang.ctc.core.ak.model.AkCallLogRecord;
import com.zifang.ctc.core.ak.model.AkIdentity;
import com.zifang.ctc.core.ak.model.AkPayload;
import com.zifang.ctc.core.ak.model.AkRecord;
import com.zifang.ctc.core.ak.model.AkScope;
import com.zifang.ctc.core.ak.model.AkScopeDomain;
import com.zifang.ctc.core.ak.model.AkScopeSaveRequest;

import java.util.List;
import java.util.Map;

/**
 * AK 台账域契约（z-ctc 拥有实现）。
 *
 * <p>业务服务（z-kb 等）不再自己存 AK：签发、验签、scope 授权、调用日志都在认证中心这边，
 * 业务侧通过 HTTP 调用过来。</p>
 *
 * @author zifang
 */
public interface AkService {

    /** 台账列表（当前操作人自建）。 */
    List<AkRecord> list(AkPayload params, AkCaller caller);

    /** 管理视角列表（租户内全部）。 */
    List<AkRecord> adminList(AkPayload params, AkCaller caller);

    /** 客户端视角列表（与管理视角同口径）。 */
    List<AkRecord> clientList(AkPayload params, AkCaller caller);

    /**
     * 签发 AK。
     *
     * <p>明文 secret 只在这一次响应里返回；库里只存 SHA-256 哈希。</p>
     */
    AkRecord create(AkPayload payload, AkCaller caller);

    /** 验签：拿明文 secret 换身份；无效抛 {@link AkException}。 */
    AkIdentity verify(String secret);

    /** scope 授权：空 scope 一律拒绝（默认拒绝，不解释成不限制）。 */
    boolean authorize(AkIdentity identity, String domain, String action, String resource);

    /** 可授权的 domain / action 目录。 */
    List<AkScopeDomain> tree(AkCaller caller);

    /** 某个 AK 的 scope 明细。 */
    Map<String, List<AkScope>> detail(String akCode, AkCaller caller, boolean admin);

    /** 保存 scope（覆盖写）。 */
    void save(AkScopeSaveRequest request, AkCaller caller);

    /** 调用日志分页。 */
    AkCallLogPage callLogs(AkCallLogQuery query, AkCaller caller);

    /** 调用日志详情（按 akCode）。 */
    AkCallLogDetail callLogDetail(String akCode, String recordId, AkCaller caller);

    /** 调用日志详情（按 akId）。 */
    AkCallLogDetail callLogDetailByAkId(String akId, String recordId, AkCaller caller);

    /** 记一条调用日志（旁路脱敏）。 */
    void recordCall(AkIdentity identity, AkCallLogInput input);
}
