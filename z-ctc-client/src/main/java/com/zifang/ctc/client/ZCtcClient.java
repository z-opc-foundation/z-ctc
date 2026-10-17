package com.zifang.ctc.client;

import com.zifang.ctc.client.model.ZCtcResource;
import com.zifang.ctc.client.model.ZCtcUserGrant;

import java.util.List;

/**
 * z-ctc 4A 中心<b>授权数据</b>的跨进程 SDK —— 回答「这个用户有哪些权限点」。
 *
 * <p>存在的理由：4A 中心自己是一个独立工作负载（k8s 里 namespace {@code z-ctc}），
 * 而消费方也各自是独立进程。以前消费方把 {@code z-ctc-web} 整份嵌进自己 JVM，
 * 于是每个进程里都长出第二份 RBAC 实现——既重复，又因为各自 {@code @MapperScan}
 * 的 bean 命名互相污染而起不来。改为各自只依赖本 SDK。
 *
 * <p><b>范围</b>：本接口只管授权数据（authz 域，读 {@code ctc_resource} /
 * {@code UserGrant} 表）。<b>登录态不归这里</b>——token 验签是
 * {@code com.zifang.ctc.sso} 的事：它本地验签或打 {@code /api/ctc/authn/verify}，
 * 那条链路不依赖数据库，与本接口的故障域必须分开。
 *
 * <p><b>与 starter 的关系</b>：本模块是<b>裸 SDK</b>，零 Spring 依赖，
 * 由 {@link ZCtcClientFactory#remote(String)} 产出可直接使用的对象。
 * 要「加依赖就得到 bean」用 {@code z-ctc-client-spring-boot-starter}，
 * 它只做装配，不含新逻辑。
 *
 * <p><b>鉴权</b>：4A 中心的 {@code SsoInterceptor} 拦截 {@code /api/**}，
 * 没有合法登录态一律 401，所以<b>每个方法都要带 token</b>，走
 * {@code Authorization: Bearer <token>}。token 由消费方从 cookie / 请求头里取
 * （那是它自己的事），SDK 只负责转发。
 */
public interface ZCtcClient {

    /** 全量权限点；调用方自己按 appCode 过滤。 */
    List<ZCtcResource> listAllResources(String token);

    /** 某用户的有效权限（角色继承 + 直挂白名单 - 直挂黑名单）。 */
    List<ZCtcResource> listUserPermissions(String token, Long userId);

    /** 某用户的直挂记录（白名单），授权管理页要做差量更新时用。 */
    List<ZCtcUserGrant> listUserGrants(String token, Long userId);

    /** 直挂一个权限点。{@code grantedBy} 冗余落库，4A 中心从 {@code X-User-Id} 头取。 */
    void grantUserResource(String token, Long userId, Long resourceId, String grantedBy);

    /** 取消直挂。 */
    void revokeUserResource(String token, Long userId, Long resourceId);
}
