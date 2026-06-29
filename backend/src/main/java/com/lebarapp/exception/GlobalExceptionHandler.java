package com.lebarapp.exception;

import com.lebarapp.dto.ApiErrorResponse;
import com.lebarapp.dto.ApiErrorResponse.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Centralized API error handling. Every handled error is rendered as a stable
 * {@link ApiErrorResponse} (timestamp, status, machine-readable code, French
 * message, request path, field errors). Unexpected failures are logged
 * server-side and returned as a generic 500 without leaking stack traces,
 * credentials, SQL or internal class names.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Expected business errors (catalog/order availability, not-found, etc.). */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex,
                                                           HttpServletRequest request) {
        ApiErrorCode code = ex.getErrorCode();
        if (code.status().is5xxServerError()) {
            log.error("Business error {} on {}: {}", code, request.getRequestURI(), ex.getMessage());
        }
        return build(code, ex.getMessage(), request, List.of());
    }

    /** Bean Validation failures on the request body (e.g. empty/too-many/null items). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getAllErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return build(ApiErrorCode.VALIDATION_ERROR, ApiErrorCode.VALIDATION_ERROR.defaultMessage(),
                request, fieldErrors);
    }

    /** Malformed JSON or an unknown enum value (e.g. an invalid size). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        return build(ApiErrorCode.MALFORMED_REQUEST, ApiErrorCode.MALFORMED_REQUEST.defaultMessage(),
                request, List.of());
    }

    /** Type mismatch on a path variable, typically a malformed tracking UUID. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                              HttpServletRequest request) {
        return build(ApiErrorCode.INVALID_IDENTIFIER, ApiErrorCode.INVALID_IDENTIFIER.defaultMessage(),
                request, List.of());
    }

    /** Unknown route or missing static resource — returns a JSON 404 instead of a 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
                                                                   HttpServletRequest request) {
        return build(ApiErrorCode.RESOURCE_NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND.defaultMessage(),
                request, List.of());
    }

    /** Last-resort handler: never leaks internals. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex,
                                                            HttpServletRequest request) {
        log.error("Unhandled error while serving {}", request.getRequestURI(), ex);
        return build(ApiErrorCode.INTERNAL_ERROR, ApiErrorCode.INTERNAL_ERROR.defaultMessage(),
                request, List.of());
    }

    private static FieldErrorDetail toFieldError(ObjectError error) {
        String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
        return new FieldErrorDetail(field, error.getDefaultMessage());
    }

    private ResponseEntity<ApiErrorResponse> build(ApiErrorCode code,
                                                   String message,
                                                   HttpServletRequest request,
                                                   List<FieldErrorDetail> fieldErrors) {
        HttpStatus status = code.status();
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                code.name(),
                message,
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
