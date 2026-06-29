package com.lebarapp.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Nimbus JOSE/JWT wiring for HS256 symmetric signing and verification. Both the
 * encoder (token creation) and decoder (token validation) share the same HMAC
 * secret externalized via {@link SecurityProperties}. No custom JWT parsing or
 * cryptography is implemented here; everything delegates to Spring Security's
 * OAuth2 Resource Server support backed by Nimbus.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JWKSource<SecurityContext> jwkSource(SecurityProperties properties) {
        return new ImmutableSecret<>(secretKey(properties));
    }

    @Bean
    public NimbusJwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public NimbusJwtDecoder jwtDecoder(SecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties)).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
        return decoder;
    }

    private static SecretKey secretKey(SecurityProperties properties) {
        byte[] keyBytes = decodeKey(properties.jwt().secret());
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Decodes the configured HMAC secret. Accepts raw UTF-8 strings of at least
     * 32 bytes (256 bits) and hex-encoded strings of at least 64 hex characters
     * (32 bytes). Shorter values are rejected with a clear startup error.
     */
    public static byte[] decodeKey(String secret) {
        byte[] keyBytes;
        if (secret.length() >= 2 && secret.length() % 2 == 0 && secret.matches("[0-9a-fA-F]+")) {
            keyBytes = HexFormat.of().parseHex(secret);
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET must be at least 256 bits (32 bytes). " +
                    "Use a longer secret or pass a 64-char hex string.");
        }
        return keyBytes;
    }
}
