package com.lebarapp.security;

import com.lebarapp.config.JwtConfig;
import com.lebarapp.config.SecurityProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates JWT secret requirements: the secret must be at least 256 bits
 * (32 bytes), whether passed as a raw UTF-8 string or as a hex string. Weak or
 * short secrets must cause a clear startup error.
 */
class JwtConfigTest {

    @Test
    void validSecretProducesDecoder() {
        SecurityProperties properties = validProperties("a-valid-secret-of-at-least-32-bytes-256b!");
        JwtConfig config = new JwtConfig();
        assertThat(config.jwtDecoder(properties)).isNotNull();
    }

    @Test
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> JwtConfig.decodeKey("short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    void hexSecretIsAcceptedIfLongEnough() {
        // 64 hex chars = 32 bytes = 256 bits
        byte[] key = JwtConfig.decodeKey(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        assertThat(key).hasSize(32);
    }

    @Test
    void shortHexSecretIsRejected() {
        // 60 hex chars = 30 bytes = 240 bits (< 256)
        assertThatThrownBy(() -> JwtConfig.decodeKey(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789ab"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    void blankSecretIsRejected() {
        assertThatThrownBy(() -> JwtConfig.decodeKey(""))
                .isInstanceOf(IllegalStateException.class);
    }

    private static SecurityProperties validProperties(String secret) {
        return new SecurityProperties(
                new SecurityProperties.Jwt("le-barapp", secret, 3600),
                List.of("http://localhost:5173"));
    }
}
