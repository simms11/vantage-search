package com.vantage.search.infrastructure;

import com.vantage.search.support.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that Spring Boot Actuator is on the classpath and the health endpoint
 * is reachable. Without spring-boot-starter-actuator, the docker-compose
 * healthcheck breaks and the Micrometer counters added for the rate limiter
 * and async executor are not scrapeable. This IT will fail the build if the
 * dependency is ever removed.
 */
class ActuatorIT extends BaseIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointIsPubliclyReachable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
