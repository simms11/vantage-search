package com.vantage.search.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashSet;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIT {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("vantage_search")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @ServiceConnection
    protected static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    static {
        if (!postgres.isRunning()) postgres.start();
        if (!redis.isRunning()) redis.start();
    }

    @Autowired
    private StringRedisTemplate redisTemplateForCleanup;

    /**
     * Delete all Redis keys matching {@code pattern} using a non-blocking SCAN
     * cursor rather than the {@code KEYS} command. KEYS is O(N) over the full
     * keyspace and is documented as production-unsafe; standardising on SCAN
     * here keeps the test/production patterns consistent.
     */
    protected void deleteRedisKeysMatching(String pattern) {
        Set<String> matches = new HashSet<>();
        try (Cursor<String> cursor = redisTemplateForCleanup.scan(
                ScanOptions.scanOptions().match(pattern).count(100).build())) {
            cursor.forEachRemaining(matches::add);
        }
        if (!matches.isEmpty()) {
            redisTemplateForCleanup.delete(matches);
        }
    }
}
