package com.lebarapp.exception;

/** Thrown when no order item matches the requested item UUID (404). */
public class OrderItemNotFoundException extends BusinessException {

    public OrderItemNotFoundException() {
        super(ApiErrorCode.ORDER_ITEM_NOT_FOUND);
    }
}
