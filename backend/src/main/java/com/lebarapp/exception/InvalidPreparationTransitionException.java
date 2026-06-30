package com.lebarapp.exception;

/**
 * Thrown when a barmaker tries to advance an order item that is already
 * {@code COMPLETED} (or otherwise cannot move to the next sequential preparation
 * step). Maps to HTTP 409 Conflict.
 */
public class InvalidPreparationTransitionException extends BusinessException {

    public InvalidPreparationTransitionException() {
        super(ApiErrorCode.INVALID_PREPARATION_TRANSITION);
    }
}
