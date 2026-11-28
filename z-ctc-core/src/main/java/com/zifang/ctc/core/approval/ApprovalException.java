package com.zifang.ctc.core.approval;

/** 审批域业务异常码段 51401 起。 */
public class ApprovalException extends RuntimeException {

    public static final int INVALID_PARAM = 51006;
    public static final int TEMPLATE_NOT_FOUND = 51401;
    public static final int TICKET_NOT_FOUND = 51402;
    public static final int STATE_CONFLICT = 51403;
    public static final int PRINCIPAL_INVALID = 51404;

    private final int code;

    public ApprovalException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ApprovalException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
