package com.lebarapp.exception;

/**
 * Base type for expected business errors that map to a specific
 * {@link ApiErrorCode} (and therefore HTTP status + machine-readable code). The
 * message carried here is the French, user-facing message returned to clients.
 */
public class BusinessException extends RuntimeException {

    private final transient ApiErrorCode errorCode;

    public BusinessException(ApiErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public BusinessException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
