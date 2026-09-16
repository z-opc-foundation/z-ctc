package com.zifang.ctc.web.service;

/**
 * 验证码下发通道 SPI.
 * <p>
 * z-ctc 是基础设施层, 不直接依赖 z-msg (业务层). 部署方 (main-starter) 按需提供实现
 * 桥接真实通道 (如 z-msg MessageGateway 的短信/邮件). 未注册任何实现时,
 * {@link VerifyCodeService} 回退为日志输出 (Mock 模式), 功能仍可用.
 */
public interface CodeChannelSender {

    /** 通道名, 与请求里的 channel 对应: PHONE / EMAIL */
    String channel();

    /**
     * 下发验证码.
     *
     * @param receiver 接收方 (手机号 / 邮箱)
     * @param scene    业务场景 (LOGIN / REGISTER / RESET)
     * @param code     验证码明文
     */
    void send(String receiver, String scene, String code);
}
