package com.zifang.ctc.core.ak;

/**
 * AK 域异常：携带业务码，供控制器转成 {@code Result.fail}。
 *
 * <p>业务码沿用既有口径（51301 AK 不存在 / 51302 AK 名重复 / 51006 参数不合法），
 * 这样门户侧看到的错误码不变。</p>
 *
 * @author zifang
 */
public class AkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 参数不合法。 */
    public static final int INVALID_PARAM = 51006;
    /** AK 不存在 / 无效。 */
    public static final int AK_NOT_FOUND = 51301;
    /** 同归属下 AK 名重复。 */
    public static final int AK_DUPLICATE_NAME = 51302;

    private final int code;

    public AkException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}
