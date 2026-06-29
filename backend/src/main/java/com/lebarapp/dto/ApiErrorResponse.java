package com.lebarapp.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Consistent error envelope returned for every handled API error. {@code code}
 * is a stable machine-readable identifier; {@code message} is a French
 * user-facing message; {@code fieldErrors} is empty unless field-level bean
 * validation failed.
 */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {

    /** A single invalid request field. */
    public record FieldErrorDetail(String field, String message) {
    }
}
