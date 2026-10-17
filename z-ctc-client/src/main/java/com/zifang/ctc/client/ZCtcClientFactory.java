package com.zifang.ctc.client;

/**
 * z-ctc 客户端工厂。
 * <pre>
 *   ZCtcClient client = ZCtcClientFactory.remote("http://z-ctc.z-ctc.svc:8888");
 * </pre>
 */
public final class ZCtcClientFactory {

    private ZCtcClientFactory() {
    }

    /**
     * @param baseUrl 4A 中心的 base URL，集群内即 {@code http://z-ctc.z-ctc.svc:8888}
     */
    public static ZCtcClient remote(String baseUrl) {
        return new RemoteZCtcClient(baseUrl);
    }
}
