package com.vantage.search.infrastructure;

import com.vantage.search.support.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseSchemaIT extends BaseIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void verifyPostgresExtensionsAreActive() {
        Integer vectorCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);

        Integer trgmCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'pg_trgm'", Integer.class);

        assertThat(vectorCount).isEqualTo(1);
        assertThat(trgmCount).isEqualTo(1);
    }
}