package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymplanner.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class BootstrapOwnerMigrationIntegrationTest extends PostgresIntegrationTest {

    private static final String KNOWN_EMAIL = "admin@gymplanner.local";
    private static final String KNOWN_PASSWORD_HASH =
            "$2a$12$4R6qzOmnCNaUu.BZUknvQOoc3khx2pQJO32mouS8JA/nljpHelqUi";
    private static final String TOMBSTONE_EMAIL = "disabled-bootstrap-owner@gymplanner.invalid";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void productionMigrationChainNeutralizesKnownBootstrapCredentialWithoutRemovingBaseData() {
        Long gymCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gyms WHERE id = 1",
                Long.class);
        Long defaultTagCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exercise_tags WHERE gym_id = 1",
                Long.class);
        Long activeKnownCredentialCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM users
                        WHERE email = ?
                          AND password_hash = ?
                          AND active IS TRUE
                        """,
                Long.class,
                KNOWN_EMAIL,
                KNOWN_PASSWORD_HASH);
        Long bootstrapUserCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = 1",
                Long.class);
        Boolean bootstrapActive = jdbcTemplate.queryForObject(
                "SELECT active FROM users WHERE id = 1",
                Boolean.class);
        String bootstrapEmail = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = 1",
                String.class);
        String bootstrapPasswordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = 1",
                String.class);

        assertThat(gymCount).isEqualTo(1L);
        assertThat(defaultTagCount).isPositive();
        assertThat(activeKnownCredentialCount).isZero();
        assertThat(bootstrapUserCount).isEqualTo(1L);
        assertThat(bootstrapActive).isFalse();
        assertThat(bootstrapEmail).isEqualTo(TOMBSTONE_EMAIL);
        assertThat(bootstrapPasswordHash).isNotEqualTo(KNOWN_PASSWORD_HASH);
    }
}
