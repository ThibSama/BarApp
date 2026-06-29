package com.lebarapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Spring context starts against a real PostgreSQL database,
 * that Flyway applied every migration, and that Hibernate {@code validate}
 * (configured via {@code ddl-auto=validate}) succeeded — the context would fail
 * to start otherwise. Also confirms the eight tables and the demo data exist.
 */
class ApplicationContextIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndFlywayApplied() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(2);
    }

    @Test
    void eightDomainTablesExist() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'app_user', 'category', 'ingredient', 'cocktail',
                    'cocktail_ingredient', 'cocktail_price',
                    'customer_order', 'order_item')
                """, Integer.class);
        assertThat(tableCount).isEqualTo(8);
    }

    @Test
    void demoCatalogIsSeeded() {
        Integer activeCategories = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category WHERE active = TRUE", Integer.class);
        Integer activeCocktails = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cocktail WHERE active = TRUE", Integer.class);

        assertThat(activeCategories).isGreaterThanOrEqualTo(3);
        assertThat(activeCocktails).isBetween(4, 6);
    }
}
