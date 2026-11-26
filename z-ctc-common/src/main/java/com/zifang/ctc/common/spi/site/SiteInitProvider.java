package com.zifang.ctc.common.spi.site;

/**
 * 建站初始化实现方接口: 一个域实现本接口, 即声明自己能替站点做哪些初始化.
 *
 * <p>契约下沉到 z-ctc-common 的原因与 {@link com.zifang.ctc.common.spi.CodeChannelSender} 同:
 * 实现方 (z-cognition / z-meta 等独立工作负载) 与消费方 (z-ctc-web) 不在同一进程树,
 * 接口挂在 z-ctc-web 上会让只引 common 的域在 classpath 扫描时读接口字节码失败.
 *
 * <p>标准协议 (域侧):
 * <ul>
 *   <li>GET  {baseUrl}/api/site-init/manifest  → SiteInitManifest</li>
 *   <li>POST {baseUrl}/api/site-init/invoke    → SiteInitInvokeResult</li>
 * </ul>
 * 端点由 z-ctc-spring-boot-starter 自动装配, 实现方只需提供本接口的 bean.
 */
public interface SiteInitProvider {

    /** 域编码 (全平台唯一), 与注册报文一致 */
    String domainCode();

    /** 域名称 (展示用) */
    default String domainName() {
        return domainCode();
    }

    /** 跨域执行顺序, 数字小者先跑; 默认 100 */
    default int order() {
        return 100;
    }

    /** 能力清单 */
    SiteInitManifest manifest();

    /**
     * 执行初始化.
     *
     * @param request 调用请求 (站点 + 能力 + 参数 + 幂等键)
     * @return 执行结果; 失败时返回 success=false 的结果对象而不是抛异常 (由 ctc 记任务失败)
     */
    SiteInitInvokeResult invoke(SiteInitInvokeRequest request);
}
