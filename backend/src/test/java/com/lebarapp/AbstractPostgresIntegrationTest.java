package com.lebarapp;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests. Starts a real PostgreSQL instance via
 * Testcontainers and wires it to Spring Boot through {@link ServiceConnection},
 * so Flyway migrations and Hibernate {@code validate} run against the same
 * engine used in production. No H2 fallback is used.
 *
 * <p><strong>Singleton container lifecycle.</strong> The container is started
 * once in a static initializer and shared by every integration-test class for
 * the whole test JVM. It is intentionally <em>not</em> managed by
 * {@code @Testcontainers}/{@code @Container}: that JUnit lifecycle stops the
 * static container in each class's {@code afterAll} and restarts it on a new
 * random port for the next class. Because every IT class shares the same
 * {@code @SpringBootTest} configuration, Spring reuses a single cached
 * application context whose {@link ServiceConnection} datasource still points at
 * the previous port — HikariCP then fails to acquire a connection and times out.
 * Starting the container once and never stopping it keeps the mapped port (and
 * therefore the cached context) valid across the entire Failsafe run. The
 * container is reclaimed by Testcontainers' Ryuk and at JVM shutdown.</p>
 *
 * <p>The {@code test} profile supplies a valid test-only JWT secret and CORS
 * configuration so that the security autoconfiguration starts without external
 * environment variables.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
