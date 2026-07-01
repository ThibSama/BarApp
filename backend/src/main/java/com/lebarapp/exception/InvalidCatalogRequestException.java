package com.lebarapp.exception;

/**
 * Thrown for semantic catalogue-request errors that bean validation cannot
 * express on its own (400): duplicate ingredient names within a request, or a
 * price set that is not exactly one active line per size S, M and L.
 */
public class InvalidCatalogRequestException extends BusinessException {

    public InvalidCatalogRequestException(String message) {
        super(ApiErrorCode.INVALID_CATALOG_REQUEST, message);
    }
}
