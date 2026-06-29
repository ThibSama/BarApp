package com.lebarapp.security;

import com.lebarapp.dto.ApiErrorResponse;
import com.lebarapp.exception.ApiErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Returns a JSON {@link ApiErrorResponse} for every unauthenticated access to a
 * protected resource, replacing the default Spring Security behaviour (which
 * would redirect to a login page or send a 403 with no body).
 *
 * <p>Exception classification is based on structured inspection of the entire
 * exception cause chain:</p>
 * <ul>
 *   <li>{@link JwtValidationException} caused by an expired-token validation
 *       error &#8594; {@code TOKEN_EXPIRED}</li>
 *   <li>Any other {@link JwtValidationException} or {@link BadJwtException}
 *       (bad signature, malformed, wrong issuer) &#8594; {@code INVALID_TOKEN}</li>
 *   <li>{@link AuthenticationServiceException} or {@link UsernameNotFoundException}
 *       from the active-user converter (disabled/deleted user)
 *       &#8594; {@code INVALID_TOKEN}</li>
 *   <li>No bearer token present at all &#8594; {@code AUTHENTICATION_REQUIRED}</li>
 * </ul>
 *
 * <p>Never reveals which specific cryptographic or structural check failed.</p>
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                          HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
                ApiErrorCode code = resolveCode(authException);
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                code.status().value(),
                code.name(),
                code.defaultMessage(),
                request.getRequestURI(),
                List.of());
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * Maps authentication exceptions to stable error codes by walking the entire
     * cause chain and checking for known exception types.
     */
    private static ApiErrorCode resolveCode(AuthenticationException ex) {
        // Walk the entire cause chain to find JWT-specific and user-status exceptions.
        for (Throwable c = ex; c != null; c = c.getCause()) {
            if (c instanceof JwtValidationException jve) {
                return isExpired(jve) ? ApiErrorCode.TOKEN_EXPIRED : ApiErrorCode.INVALID_TOKEN;
            }
            if (c instanceof BadJwtException) {
                return ApiErrorCode.INVALID_TOKEN;
            }
            // BadCredentialsException: thrown by ActiveUserJwtAuthenticationConverter
            // when the user is disabled/deleted.
            if (c instanceof org.springframework.security.authentication.BadCredentialsException) {
                return ApiErrorCode.INVALID_TOKEN;
            }
            // UsernameNotFoundException: thrown by BarmakerUserDetailsService when
            // the user referenced by the JWT no longer exists.
            if (c instanceof UsernameNotFoundException) {
                return ApiErrorCode.INVALID_TOKEN;
            }
        }

        // If a bearer token was supplied but invalid (non-expiry), return INVALID_TOKEN
        if (ex instanceof InvalidBearerTokenException) {
            return ApiErrorCode.INVALID_TOKEN;
        }

        // No token at all, or any other authentication failure without a token
        return ApiErrorCode.AUTHENTICATION_REQUIRED;
    }

    /**
     * Determines whether a {@link JwtValidationException} was caused by token
     * expiry. Inspects the collection of validation errors and falls back to
     * message inspection only as a last resort.
     */
    private static boolean isExpired(JwtValidationException ex) {
        Object errors = ex.getErrors();
        if (errors != null) {
            String errorsStr = errors.toString();
            if (errorsStr.contains("expired")) {
                return true;
            }
        }
        String message = ex.getMessage();
        return message != null && message.contains("expired");
    }
}
