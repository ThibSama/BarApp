package com.lebarapp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Typed security configuration properties for JWT issuance, resource-server
 * validation and CORS. Bound from environment variables via relaxed binding
 * (e.g. {@code APP_JWT_SECRET} &#8594; {@code app.jwt.secret}) so no {@code @Value}
 * injection is scattered across components.
 *
 * <p>{@link Validated} enforces bean-validation constraints at startup: a missing
 * or blank {@code APP_JWT_SECRET} causes a clear configuration error instead of
 * a late runtime failure.</p>
 */
@ConfigurationProperties(prefix = "app")
@Validated
public record SecurityProperties(
        @NotNull @Valid Jwt jwt,
        @NotEmpty List<String> corsAllowedOrigins) {

    /**
     * JWT issuer, signing secret and token lifetime.
     *
     * @param issuer            token {@code iss} claim and validation audience
     * @param secret            HMAC-SHA signing key (minimum 256 bits / 32 bytes)
     * @param expirationSeconds access-token lifetime
     */
    public record Jwt(
            @NotBlank String issuer,
            @NotBlank String secret,
            @Min(1) long expirationSeconds) {
    }
}
