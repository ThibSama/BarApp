package com.lebarapp.security;

import com.lebarapp.config.SecurityProperties;
import com.lebarapp.entity.AppUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Issues signed HS256 access tokens using {@link NimbusJwtEncoder}. Claims
 * include {@code sub}, {@code userId}, {@code role}, {@code iat}, {@code exp}
 * and {@code iss}. No password, hash or personal data is ever embedded. Refresh
 * tokens are intentionally not implemented in the MVP.
 */
@Service
public class JwtTokenService {

    private static final String TOKEN_TYPE = "Bearer";

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    public JwtTokenService(JwtEncoder jwtEncoder, SecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /**
     * Builds and signs a new access token for the given barmaker.
     *
     * @return the token, its type and its expiration (seconds)
     */
    public TokenResult createToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.jwt().expirationSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
        return new TokenResult(jwt.getTokenValue(), TOKEN_TYPE,
                properties.jwt().expirationSeconds());
    }

    /**
     * Issued access token result exposed to the login response DTO.
     *
     * @param accessToken    signed JWT
     * @param tokenType      always {@code Bearer}
     * @param expirationSeconds token lifetime
     */
    public record TokenResult(String accessToken, String tokenType, long expirationSeconds) {
    }
}
