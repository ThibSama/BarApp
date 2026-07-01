package com.lebarapp.security;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.dto.LoginResponse;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import com.lebarapp.config.SecurityProperties;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the manager-only staff administration API
 * ({@code /api/bar/users}) against a real PostgreSQL database. Validates the full
 * authorization matrix (anonymous 401, barmaker 403, manager allowed), account
 * creation (always role BARMAKER, active, BCrypt-hashed password, no hash
 * leakage), case-insensitive username conflicts (409) and that a manager retains
 * access to every existing barmaker feature.
 */
class UserAdminIT extends AbstractPostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private SecurityProperties properties;

    private static final String BARMAKER_USERNAME = "barmaker";
    private static final String BARMAKER_PASSWORD = "barmaker-test";
    private static final String MANAGER_USERNAME = "manager";
    private static final String MANAGER_PASSWORD = "manager-test";

    /** Remove any account created by a test, leaving the two demo accounts intact. */
    @AfterEach
    void cleanUpCreatedAccounts() {
        jdbcTemplate.update(
                "DELETE FROM app_user WHERE LOWER(username) NOT IN ('barmaker', 'manager')");
    }

    // ----------------------------------------------------------------
    // Seed / migration
    // ----------------------------------------------------------------

    @Test
    void demoManagerExistsAfterFlywayWithBcryptHash() {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT role, active, password_hash FROM app_user WHERE LOWER(username) = 'manager'");
        assertThat(row.get("role")).isEqualTo("MANAGER");
        assertThat(row.get("active")).isEqualTo(true);
        String hash = (String) row.get("password_hash");
        assertThat(hash).startsWith("$2a$10$").hasSize(60);
        assertThat(passwordEncoder.matches(MANAGER_PASSWORD, hash)).isTrue();
    }

    @Test
    void demoBarmakerRemainsUnchangedBarmaker() {
        String role = jdbcTemplate.queryForObject(
                "SELECT role FROM app_user WHERE LOWER(username) = 'barmaker'", String.class);
        assertThat(role).isEqualTo("BARMAKER");
    }

    @Test
    void managerLoginReturnsManagerRole() {
        ResponseEntity<LoginResponse> response = login(MANAGER_USERNAME, MANAGER_PASSWORD);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().user().role().name()).isEqualTo("MANAGER");
    }

    // ----------------------------------------------------------------
    // Authorization matrix
    // ----------------------------------------------------------------

    @Test
    void anonymousGetUsersReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity(url("/api/bar/users"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anonymousPostUsersReturns401() {
        ResponseEntity<Map> response = restTemplate.exchange(url("/api/bar/users"), HttpMethod.POST,
                new HttpEntity<>(createBody("Anon", "anon.user", "password123"), jsonHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void barmakerGetUsersReturns403() {
        String token = loginAndGetToken(BARMAKER_USERNAME, BARMAKER_PASSWORD);
        ResponseEntity<Map> response = getWithToken("/api/bar/users", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void barmakerPostUsersReturns403() {
        String token = loginAndGetToken(BARMAKER_USERNAME, BARMAKER_PASSWORD);
        ResponseEntity<Map> response = postWithToken("/api/bar/users",
                createBody("Blocked", "blocked.user", "password123"), token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("ACCESS_DENIED");
        assertThat(userExists("blocked.user")).isFalse();
    }

    @Test
    void managerGetUsersReturnsStaffWithoutPasswordData() {
        String token = loginAndGetToken(MANAGER_USERNAME, MANAGER_PASSWORD);
        ResponseEntity<String> response = restTemplate.exchange(url("/api/bar/users"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).contains("barmaker").contains("manager");
        assertThat(body).doesNotContain("password");
        assertThat(body).doesNotContain("passwordHash");
        assertThat(body).doesNotContain("$2a$");
    }

    // ----------------------------------------------------------------
    // Creation
    // ----------------------------------------------------------------

    @Test
    void managerCreatesBarmakerReturns201WithSafeBody() {
        String token = loginAndGetToken(MANAGER_USERNAME, MANAGER_PASSWORD);
        ResponseEntity<String> response = restTemplate.exchange(url("/api/bar/users"), HttpMethod.POST,
                new HttpEntity<>(createBody("Alice Martin", "alice", "temporary-password"), authHeaders(token)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().getPath()).matches("/api/bar/users/\\d+");
        String body = response.getBody();
        assertThat(body).contains("\"role\":\"BARMAKER\"");
        assertThat(body).contains("\"active\":true");
        assertThat(body).doesNotContain("password");
        assertThat(body).doesNotContain("$2a$");
    }

    @Test
    void createdPasswordIsBcryptHashedAndNotPlaintext() {
        String token = loginAndGetToken(MANAGER_USERNAME, MANAGER_PASSWORD);
        postWithTokenRaw(createBody("Bob", "bob", "s3cr3t-password"), token);

        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM app_user WHERE LOWER(username) = 'bob'", String.class);
        assertThat(hash).startsWith("$2a$").hasSize(60);
        assertThat(hash).doesNotContain("s3cr3t-password");
        assertThat(passwordEncoder.matches("s3cr3t-password", hash)).isTrue();
    }

    @Test
    void createdBarmakerCanLoginAndAccessBarmakerFeaturesButNotUsers() {
        String managerToken = loginAndGetToken(MANAGER_USERNAME, MANAGER_PASSWORD);
        postWithTokenRaw(createBody("Carol", "carol", "carol-password"), managerToken);

        ResponseEntity<LoginResponse> loginResponse = login("carol", "carol-password");
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody().user().role().name()).isEqualTo("BARMAKER");

        String carolToken = loginResponse.getBody().accessToken();
        // Normal barmaker features work.
        assertThat(restTemplate.exchange(url("/api/bar/categories"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(carolToken)), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        // Staff management is forbidden.
        assertThat(getWithToken("/api/bar/users", carolToken).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void roleFieldInPayloadIsIgnoredAndAccountStaysBarmaker() {
        String token = loginAndGetToken(MANAGER_USERNAME, MANAGER_PASSWORD);
        String maliciousBody = """
                {"displayName":"Eve","username":"eve","password":"eve-password",
                 "role":"MANAGER","active":false}""";
        ResponseEntity<String> response = restTemplate.exchange(url("/api/bar/users"), HttpMethod.POST,
                new HttpEntity<>(maliciousBody, authHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"role\":\"BARMAKER\"").contains("\"active\":true");
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT role, active FROM app_user WHERE LOWER(username) = 'eve'");
        assertThat(row.get("role")).isEqualTo("BARMAKER");
        assertThat(row.get("active")).isEqualTo(true);
    }

    @Test
    void tamperedJwtRoleClaimCannotGrantManagerAccess() {
        // Forge a correctly-signed token for the demo barmaker but claim MANAGER.
        // Authorities are reloaded from PostgreSQL, so the DB role (BARMAKER)
        // wins and the users endpoint stays forbidden.
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(BARMAKER_USERNAME)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", "MANAGER")
                .build();
        String forged = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        ResponseEntity<Map> response = getWithToken("/api/bar/users", forged);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ----------------------------------------------------------------
    // Username conflicts
    // ----------------------------------------------------------------

    @Test
    void duplicateUsernameDifferentCasingReturns409AndCreatesNoSecondAccount() {
        String token = loginAndGetToken(MANAGER_USERNAME, MANAGER_PASSWORD);
        assertThat(postWithToken("/api/bar/users", createBody("Dan", "dan", "dan-password"), token)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);

        for (String variant : new String[]{"dan", "DAN", "Dan"}) {
            ResponseEntity<Map> response = postWithToken("/api/bar/users",
                    createBody("Dan Two", variant, "other-password"), token);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().get("code")).isEqualTo("USERNAME_ALREADY_EXISTS");
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE LOWER(username) = 'dan'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // ----------------------------------------------------------------
    // Validation
    // ----------------------------------------------------------------

    @Test
    void blankDisplayNameReturns400() {
        assertValidationRejected(createBody("  ", "validname", "password123"));
    }

    @Test
    void invalidUsernameReturns400() {
        assertValidationRejected(createBody("Name", "has spaces!", "password123"));
    }

    @Test
    void tooShortUsernameReturns400() {
        assertValidationRejected(createBody("Name", "ab", "password123"));
    }

    @Test
    void tooShortPasswordReturns400() {
        assertValidationRejected(createBody("Name", "shortpw", "short7!"));
    }

    @Test
    void tooLongPasswordReturns400() {
        String longPassword = "a".repeat(73);
        assertValidationRejected(createBody("Name", "longpw", longPassword));
    }

    // ----------------------------------------------------------------
    // Manager keeps barmaker features
    // ----------------------------------------------------------------

    @Test
    void managerKeepsAccessToOrdersCategoriesAndCocktails() {
        String token = loginAndGetToken(MANAGER_USERNAME, MANAGER_PASSWORD);
        assertThat(getListStatus("/api/bar/orders", token)).isEqualTo(HttpStatus.OK);
        assertThat(getListStatus("/api/bar/categories", token)).isEqualTo(HttpStatus.OK);
        assertThat(getListStatus("/api/bar/cocktails", token)).isEqualTo(HttpStatus.OK);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void assertValidationRejected(String body) {
        String token = loginAndGetToken(MANAGER_USERNAME, MANAGER_PASSWORD);
        ResponseEntity<Map> response = postWithToken("/api/bar/users", body, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("VALIDATION_ERROR");
    }

    private boolean userExists(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE LOWER(username) = LOWER(?)", Integer.class, username);
        return count != null && count > 0;
    }

    private String createBody(String displayName, String username, String password) {
        return "{\"displayName\":\"" + displayName + "\",\"username\":\"" + username
                + "\",\"password\":\"" + password + "\"}";
    }

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

    private ResponseEntity<LoginResponse> login(String username, String password) {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        return restTemplate.exchange(url("/api/auth/login"), HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders()), LoginResponse.class);
    }

    private String loginAndGetToken(String username, String password) {
        return login(username, password).getBody().accessToken();
    }

    private ResponseEntity<Map> getWithToken(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);
    }

    private HttpStatus getListStatus(String path, String token) {
        return (HttpStatus) restTemplate.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), String.class).getStatusCode();
    }

    private ResponseEntity<Map> postWithToken(String path, String body, String token) {
        return restTemplate.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    private void postWithTokenRaw(String body, String token) {
        restTemplate.exchange(url("/api/bar/users"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), String.class);
    }
}
