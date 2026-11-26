package com.zifang.ctc.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2 授权服务器的接入方（client）注册表 —— <b>配置表</b>方案。
 *
 * <p>2026-11-01 与 CEO 对齐：接入方目前只有 z-biz-tool 家族（remote / mind 等），
 * 建数据库表 + 管理端点是杀鸡用牛刀；先配置表，将来接入方多了再升级。
 *
 * <p>YAML 形态（松散绑定：{@code z.ctc.oauth.clients[0].client-id}）：
 * <pre>
 * z:
 *   ctc:
 *     oauth:
 *       clients:
 *         - client-id: z-biz-tool-remote
 *           redirect-uris:
 *             - http://127.0.0.1:17888/callback   # 精确匹配（loopback + 固定端口）
 *           scope: openid profile email
 * </pre>
 *
 * <p><b>redirect_uri 是精确匹配，不是前缀匹配</b> —— OAuth2 安全基线：
 * 前缀匹配下 {@code http://127.0.0.1:17888/callback.evil.com} 这类地址能骗过登记。
 * loopback 动态端口的特例（RFC 8252 §7.3）：登记形如 {@code http://127.0.0.1:STAR/callback}
 * （端口位写 STAR，即字面星号占位）时，只匹配 scheme/host/path、<b>任意端口</b> ——
 * 这是给桌面客户端「端口由内核分配」的形态留的口子，host 与 path 仍然严格。
 *
 * <p>本属性类 <b>必须注册为 Bean</b>（见 {@code z-ctc-web} 的自动配置）；
 * clients 为空 = 没有接入方 = 三个端点全部拒绝 —— 「未配置」是一等状态，
 * 与家族客户端「不显示 SSO 入口」的设计互相咬合。
 */
@ConfigurationProperties(prefix = "z.ctc.oauth")
public class OAuthClientProperties {

    private List<Client> clients = new ArrayList<>();

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    /**
     * 按 client_id 找注册项；没注册返回 null（调用方按「无效 client」拒绝）。
     */
    public Client find(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            return null;
        }
        for (Client c : clients) {
            if (clientId.equals(c.getClientId())) {
                return c;
            }
        }
        return null;
    }

    public static class Client {

        /** 接入方的公开标识（如 {@code z-biz-tool-remote}）。 */
        private String clientId;

        /**
         * 允许的回调地址白名单。<b>精确匹配</b>；
         * 端口位写 STAR（字面星号占位，注释里不便直接写星号）表示 loopback 任意端口（见类注释）。
         */
        private List<String> redirectUris = new ArrayList<>();

        /** 允许的 scope 空间（空格分隔）。授权请求的 scope 必须是它的子集。 */
        private String scope = "openid profile email";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public List<String> getRedirectUris() {
            return redirectUris;
        }

        public void setRedirectUris(List<String> redirectUris) {
            this.redirectUris = redirectUris;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
