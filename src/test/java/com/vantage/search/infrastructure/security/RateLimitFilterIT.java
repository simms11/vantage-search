package com.vantage.search.infrastructure.security;

import com.vantage.search.support.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Lua-backed atomic rate limiter rejects with 429 once capacity is reached.
 * Default test capacity is 10000 (see application-test.properties) so other ITs don't
 * trip the limiter; this test overrides to a small capacity for fast determinism.
 */
@TestPropertySource(properties = {
        "app.rate-limit.capacity=10",
        "app.rate-limit.refill-duration-seconds=60"
})
class RateLimitFilterIT extends BaseIT {

    private static final int CAPACITY = 10;

    // Empty credentials provoke @NotBlank validation -> 400 (instead of 401 with
    // WWW-Authenticate, which makes HttpURLConnection attempt an un-retryable retry).
    private static final Map<String, String> EMPTY_CREDS = Map.of(
            "username", "",
            "password", "");

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void clearRateLimitState() {
        deleteRedisKeysMatching("rate-limit:*");
    }

    @Test
    void rejectsWith429OnceCapacityIsReached() {
        for (int i = 1; i <= CAPACITY; i++) {
            HttpStatusCode status = restTemplate
                    .postForEntity("/api/auth/login", EMPTY_CREDS, String.class)
                    .getStatusCode();
            assertThat(status)
                    .as("request %d (within capacity) must not be rate-limited", i)
                    .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        ResponseEntity<String> overLimit = restTemplate.postForEntity(
                "/api/auth/login", EMPTY_CREDS, String.class);
        assertThat(overLimit.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
