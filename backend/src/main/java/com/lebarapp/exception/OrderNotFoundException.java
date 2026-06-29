package com.lebarapp.exception;

/** Thrown when no order matches the requested tracking UUID (404). */
public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException() {
        super(ApiErrorCode.ORDER_NOT_FOUND);
    }
}
