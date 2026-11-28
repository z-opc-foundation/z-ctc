package com.zifang.ctc.core.service.session;

/**
 * 机器 / 服务账号会话（认证中心持有真源）。
 *
 * <p>签发 / 验签 / 吊销都在认证中心，业务域（BIOS 控制台等）只调用，
 * 不再自建令牌表与自造校验拦截器。令牌只存 SHA-256 哈希，明文只在签发那一次响应返回。</p>
 */
public interface ServiceSessionService {

    /** 签发令牌；返回的 {@code token} 只在这一次响应里出现。 */
    ServiceSession issue(String tenantCode, String subjectType, String subjectCode, long ttlMillis);

    /** 验签：令牌无效 / 过期 / 已吊销都返回 null（调用方按业务口径拒）。 */
    ServiceSession verify(String token);

    /** 吊销；令牌不存在时返回 false。 */
    boolean revoke(String token);

    /** 当前有效会话数（按租户）。 */
    int activeCount(String tenantCode);
}
