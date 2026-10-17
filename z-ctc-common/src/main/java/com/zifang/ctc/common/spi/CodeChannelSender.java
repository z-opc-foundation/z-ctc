package com.zifang.ctc.common.spi;

/**
 * 验证码下发通道 SPI.
 * <p>
 * 放在 common 而不是 z-ctc-web: 实现方 (z-msg 的 MsgCodeChannelSenders) 与消费方
 * (z-ctc-web 的 VerifyCodeService) 不在同一进程树里 —— 4A 中心自成分离工作负载后,
 * z-msg 使用方若 implements 挂在 z-ctc-web 上的接口, 只要 classpath 里没有 z-ctc-web
 * (z-ctc-web 对它是 provided), ConfigurationClassParser 读接口字节码就会
 * FileNotFoundException, 容器连 bean 都建不起来。契约下沉到两边都必有的 common。
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
