package com.zifang.ctc.client;

/**
 * z-ctc SDK 的唯一异常类型。消费方 catch 它就知道是 4A 中心这一侧的问题
 * （网络不通、非 2xx、业务失败），与其他故障区分得开。
 */
public class ZCtcClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ZCtcClientException(String message) {
        super(message);
    }

    public ZCtcClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
