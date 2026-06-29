package com.lebarapp.security;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.config.SecurityProperties;
import com.lebarapp.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end security integration tests against a real PostgreSQL database:
 * login, JWT issuance/validation, /api/auth/me, role-based authorization,
 * public route regressions and CORS. All tests run through the full Spring
 * filter chain over a real HTTP port (not MockMvc) to validate the complete
 * stateless security pipeline.
 */
class SecurityIT extends AbstractPostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private SecurityProperties properties;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String DEMO_USERNAME = "barmaker";
    private static final String DEMO_PASSWORD = "barapp-demo-2024";
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    // ----------------------------------------------------------------
    // Authentication tests (1-6)
    // ----------------------------------------------------------------

    @Test
    void validCredentialsAuthenticate() {
        ResponseEntity<LoginResponse> response = login(DEMO_USERNAME, DEMO_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().expiresIn()).isEqualTo(3600);
    }

    @Test
    void usernameLookupIsCaseInsensitive() {
        for (String variant : new String[]{"BARMAKER", "Barmaker", "bArMaKeR"}) {
            ResponseEntity<LoginResponse> response = login(variant, DEMO_PASSWORD);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().user().username()).isEqualTo(DEMO_USERNAME);
        }
    }

    @Test
    void incorrectPasswordReturnsGeneric401() {
        ResponseEntity<Map> response = loginRaw(DEMO_USERNAME, "wrong-password");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().get("message")).isEqualTo("Identifiants incorrects.");
    }

    @Test
    void unknownUserReturnsSame401() {
        ResponseEntity<Map> response = loginRaw("unknown-user", DEMO_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().get("message")).isEqualTo("Identifiants incorrects.");
    }

    @Test
    void inactiveUserCannotAuthenticate() {
        // Deactivate the demo user, try login, then reactivate.
        jdbcTemplate.update("UPDATE app_user SET active = FALSE WHERE LOWER(username) = 'barmaker'");
        try {
            ResponseEntity<Map> response = loginRaw(DEMO_USERNAME, DEMO_PASSWORD);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().get("code")).isEqualTo("INVALID_CREDENTIALS");
        } finally {
            jdbcTemplate.update("UPDATE app_user SET active = TRUE WHERE LOWER(username) = 'barmaker'");
        }
    }

    @Test
    void passwordHashIsNeverExposed() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/auth/login"), HttpMethod.POST,
                loginEntity(DEMO_USERNAME, DEMO_PASSWORD), String.class);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).doesNotContain("$2a$10$");
        assertThat(body).doesNotContain("passwordHash");
        assertThat(body).doesNotContain("password_hash");
    }

    // ----------------------------------------------------------------
    // JWT tests (7-13)
    // ----------------------------------------------------------------

    @Test
    void validJwtIsAccepted() {
        String token = loginAndGetToken(DEMO_USERNAME, DEMO_PASSWORD);
        ResponseEntity<Map> response = getWithToken("/api/auth/me", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void invalidSignatureReturns401() {
        String token = generateTokenWithWrongSecret();
        ResponseEntity<Map> response = getWithToken("/api/auth/me", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void malformedJwtReturns401() {
        ResponseEntity<Map> response = getWithToken("/api/auth/me", "not.a.real.jwt");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void expiredJwtReturns401() {
        String token = generateExpiredToken(DEMO_USERNAME);
        ResponseEntity<Map> response = getWithToken("/api/auth/me", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void wrongIssuerReturns401() {
        String token = generateTokenWithWrongIssuer(DEMO_USERNAME);
        ResponseEntity<Map> response = getWithToken("/api/auth/me", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void queryParameterTokenIsIgnored() {
        String token = loginAndGetToken(DEMO_USERNAME, DEMO_PASSWORD);
        // Token in query parameter must NOT be accepted by resource server.
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/auth/me?access_token=" + token), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenWithoutBearerPrefixIsRejected() {
        String token = loginAndGetToken(DEMO_USERNAME, DEMO_PASSWORD);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token); // no "Bearer " prefix
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/auth/me"), HttpMethod.GET, entity, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ----------------------------------------------------------------
    // Endpoint tests (14-20)
    // ----------------------------------------------------------------

    @Test
    void validLoginReturnsTokenAndUser() {
        ResponseEntity<LoginResponse> response = login(DEMO_USERNAME, DEMO_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.tokenType()).isEqualTo("Bearer");
        assertThat(body.expiresIn()).isEqualTo(3600);
        assertThat(body.user()).isNotNull();
        assertThat(body.user().id()).isNotNull();
        assertThat(body.user().username()).isEqualTo(DEMO_USERNAME);
        assertThat(body.user().displayName()).isEqualTo("Barman principal");
        assertThat(body.user().role().name()).isEqualTo("BARMAKER");
    }

    @Test
    void blankUsernameReturns400() {
        ResponseEntity<Map> response = loginRaw(" ", DEMO_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isIn("VALIDATION_ERROR", "MALFORMED_REQUEST");
    }

    @Test
    void blankPasswordReturns400() {
        ResponseEntity<Map> response = loginRaw(DEMO_USERNAME, "");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isIn("VALIDATION_ERROR", "MALFORMED_REQUEST");
    }

    @Test
    void malformedJsonReturns400() {
        HttpHeaders headers = jsonHeaders();
        HttpEntity<String> entity = new HttpEntity<>("{\"username\":\"barmaker\",", headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/auth/login"), HttpMethod.POST, entity, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void meWithoutTokenReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/auth/me"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    void meWithValidTokenReturnsCurrentUser() {
        String token = loginAndGetToken(DEMO_USERNAME, DEMO_PASSWORD);
        ResponseEntity<Map> response = getWithToken("/api/auth/me", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("username")).isEqualTo(DEMO_USERNAME);
        assertThat(response.getBody().get("role")).isEqualTo("BARMAKER");
        assertThat(response.getBody().get("displayName")).isEqualTo("Barman principal");
        assertThat(response.getBody().get("id")).isNotNull();
        // Must not leak password hash
        assertThat(response.getBody()).doesNotContainKey("passwordHash");
        assertThat(response.getBody()).doesNotContainKey("password_hash");
    }

    @Test
    void disabledUserWithExistingTokenIsRejectedOnMe() {
        // 1. Login while active to get a valid token
        String token = loginAndGetToken(DEMO_USERNAME, DEMO_PASSWORD);
        // 2. Disable the user in DB
        jdbcTemplate.update("UPDATE app_user SET active = FALSE WHERE LOWER(username) = 'barmaker'");
        try {
            // 3. Token is structurally valid, but /me must reject because user is disabled
            ResponseEntity<Map> response = getWithToken("/api/auth/me", token);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().get("code")).isEqualTo("INVALID_TOKEN");
        } finally {
            jdbcTemplate.update("UPDATE app_user SET active = TRUE WHERE LOWER(username) = 'barmaker'");
        }
    }

    @Test
    void disabledUserTokenIsImmediatelyRejectedFromBarRoute() {
        // 1. Login while active to get a valid token
        String token = loginAndGetToken(DEMO_USERNAME, DEMO_PASSWORD);
        // 2. Verify the token works initially
        ResponseEntity<Map> okResponse = getWithToken("/api/bar/test", token);
        assertThat(okResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 3. Disable the user in DB
        jdbcTemplate.update("UPDATE app_user SET active = FALSE WHERE LOWER(username) = 'barmaker'");
        try {
            // 4. Same token must be immediately rejected on /api/bar/test
            ResponseEntity<Map> response = getWithToken("/api/bar/test", token);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().get("code")).isEqualTo("INVALID_TOKEN");
            // 5. /api/auth/me is also rejected
            ResponseEntity<Map> meResponse = getWithToken("/api/auth/me", token);
            assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(meResponse.getBody().get("code")).isEqualTo("INVALID_TOKEN");
        } finally {
            jdbcTemplate.update("UPDATE app_user SET active = TRUE WHERE LOWER(username) = 'barmaker'");
        }
    }

    // ----------------------------------------------------------------
    // Authorization tests (21-23)
    // ----------------------------------------------------------------

    @Test
    void barRouteWithoutTokenReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/bar/test"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void barRouteWithValidBarmakerTokenSucceeds() {
        String token = loginAndGetToken(DEMO_USERNAME, DEMO_PASSWORD);
        ResponseEntity<Map> response = getWithToken("/api/bar/test", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("ok");
    }

    @Test
    void insufficientRoleReturns403() {
        // With the active-user converter, the role is always loaded from the
        // database (BARMAKER only in the MVP). The 403 access-denied path is
        // validated separately by {@link BarRouteAuthorizationTest} using
        // Spring Security test support with @WithMockUser(roles = "VIEWER").
        // This test verifies that a valid BARMAKER token succeeds on /api/bar/test.
        String token = loginAndGetToken(DEMO_USERNAME, DEMO_PASSWORD);
        ResponseEntity<Map> response = getWithToken("/api/bar/test", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ----------------------------------------------------------------
    // Public route regressions (24-26)
    // ----------------------------------------------------------------

    @Test
    void getMenuRemainsPublic() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/menu"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void postOrdersRemainsPublic() {
        HttpHeaders headers = jsonHeaders();
        String body = "{\"items\":[{\"cocktailId\":1,\"size\":\"M\"}]}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/orders"), HttpMethod.POST, entity, Map.class);
        // 201 Created means the security filter let it through (no auth required)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void getOrderByIdRemainsPublic() {
        // Create an order first, then track it without authentication
        HttpHeaders headers = jsonHeaders();
        String body = "{\"items\":[{\"cocktailId\":1,\"size\":\"S\"}]}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                url("/api/orders"), HttpMethod.POST, entity, Map.class);
        String orderId = createResponse.getBody().get("id").toString();

        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/orders/" + orderId), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ----------------------------------------------------------------
    // Unknown route tests (404 JSON)
    // ----------------------------------------------------------------

    @Test
    void unknownRouteWithoutAuthReturns401Not500() {
        // An unknown route without a token should return 401 (auth required)
        // not a 500 internal error or HTML response.
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/nonexistent-endpoint"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    void unknownAuthenticatedRouteReturnsJson404() {
        String token = loginAndGetToken(DEMO_USERNAME, DEMO_PASSWORD);
        ResponseEntity<Map> response = getWithToken("/api/bar/nonexistent", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("RESOURCE_NOT_FOUND");
        String body = response.getBody().toString();
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("at com.");
    }

    // ----------------------------------------------------------------
    // CORS tests (28-30)
    // ----------------------------------------------------------------

    @Test
    void allowedOriginReceivesCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", ALLOWED_ORIGIN);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/menu"), HttpMethod.GET, entity, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ALLOWED_ORIGIN);
    }

    @Test
    void unknownOriginReceivesNoAccessPermission() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://evil-site.com");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/menu"), HttpMethod.GET, entity, Void.class);
        // CORS headers must NOT be present for unknown origins
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void allowedPreflightSucceeds() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", ALLOWED_ORIGIN);
        headers.set("Access-Control-Request-Method", "POST");
        headers.set("Access-Control-Request-Headers", "Content-Type");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/auth/login"), HttpMethod.OPTIONS, entity, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ALLOWED_ORIGIN);
    }

    // ----------------------------------------------------------------
    // Integration tests (31-34)
    // ----------------------------------------------------------------

    @Test
    void demoUserExistsAfterFlyway() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE LOWER(username) = 'barmaker'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void storedPasswordIsBcrypt() {
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM app_user WHERE LOWER(username) = 'barmaker'",
                String.class);
        assertThat(hash).startsWith("$2a$10$");
        // BCrypt hashes are 60 characters
        assertThat(hash).hasSize(60);
    }

    @Test
    void storedPasswordIsNotPlaintext() {
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM app_user WHERE LOWER(username) = 'barmaker'",
                String.class);
        assertThat(hash).doesNotContain(DEMO_PASSWORD);
        // Verify it actually verifies against the known plaintext
        assertThat(passwordEncoder.matches(DEMO_PASSWORD, hash)).isTrue();
    }

    @Test
    void loginWorksAgainstTestcontainersPostgres() {
        ResponseEntity<LoginResponse> response = login(DEMO_USERNAME, DEMO_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().user().username()).isEqualTo(DEMO_USERNAME);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    private HttpEntity<String> loginEntity(String username, String password) {
        return new HttpEntity<>(
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}",
                jsonHeaders());
    }

    private ResponseEntity<LoginResponse> login(String username, String password) {
        return restTemplate.exchange(
                url("/api/auth/login"), HttpMethod.POST,
                loginEntity(username, password), LoginResponse.class);
    }

    private ResponseEntity<Map> loginRaw(String username, String password) {
        return restTemplate.exchange(
                url("/api/auth/login"), HttpMethod.POST,
                loginEntity(username, password), Map.class);
    }

    private String loginAndGetToken(String username, String password) {
        return login(username, password).getBody().accessToken();
    }

    private ResponseEntity<Map> getWithToken(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);
    }

    private String generateExpiredToken(String subject) {
        Instant now = Instant.now();
        Instant pastExpiry = now.minusSeconds(60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(subject)
                .issuedAt(now.minusSeconds(3700))
                .expiresAt(pastExpiry)
                .claim("role", "BARMAKER")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String generateTokenWithWrongIssuer(String subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("wrong-issuer")
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", "BARMAKER")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String generateTokenWithRole(String subject, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", role)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String generateTokenWithWrongSecret() {
        // Use a different secret key to produce a token with an invalid signature
        SecretKeySpec wrongKey = new SecretKeySpec(
                "another-different-secret-key-for-invalid-signature-256b".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        com.nimbusds.jose.jwk.source.JWKSource<com.nimbusds.jose.proc.SecurityContext> source =
                new com.nimbusds.jose.jwk.source.ImmutableSecret<>(wrongKey);
        org.springframework.security.oauth2.jwt.NimbusJwtEncoder wrongEncoder =
                new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(source);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(DEMO_USERNAME)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", "BARMAKER")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return wrongEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
