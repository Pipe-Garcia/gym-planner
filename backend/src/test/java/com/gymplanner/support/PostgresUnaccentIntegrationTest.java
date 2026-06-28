package com.gymplanner.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresUnaccentIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unaccentExtensionIsAvailableAfterRealMigrationsRun() {
        String result = jdbcTemplate.queryForObject("select unaccent('Canci\u00f3n')", String.class);

        assertThat(result).isEqualTo("Cancion");
    }
}
