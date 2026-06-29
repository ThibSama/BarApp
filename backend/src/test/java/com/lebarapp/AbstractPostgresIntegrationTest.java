package com.lebarapp;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests. Starts a real PostgreSQL instance via
 * Testcontainers and wires it to Spring Boot through {@link ServiceConnection},
 * so Flyway migrations and Hibernate {@code validate} run against the same
 * engine used in production. No H2 fallback is used.
 *
 * <p>The {@code test} profile supplies a valid test-only JWT secret and CORS
 * configuration so that the security autoconfiguration starts without external
 * environment variables.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");
}
