package com.vantage.search.infrastructure.security;

import com.vantage.search.support.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of the JWT authentication lifecycle:
 *  - login mints a token,
 *  - bearer header authenticates a protected request,
 *  - logout revokes the token,
 *  - reusing the revoked token returns 401,
 *  - missing or malformed bearer returns 401.
 *
 * Exercises the real {@code JwtAuthenticationFilter} chain (no @WithMockUser shortcut).
 */
class AuthFlowIT extends BaseIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void clearRedisState() {
        deleteRedisKeysMatching("rate-limit:*");
        deleteRedisKeysMatching("jwt:revoked:*");
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginThenAuthenticatedCallThenLogoutInvalidatesToken() {
        // Login with the bootstrapped admin (see application-test.properties).
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("username", "admin", "password", "test-password"),
                Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) loginResponse.getBody().get("token");
        assertThat(token).isNotBlank();

        UUID nonexistentClient = UUID.randomUUID();
        HttpEntity<Void> bearerRequest = new HttpEntity<>(bearerHeaders(token));

        // Bearer reaches the controller: 404 (not 401) proves authentication succeeded.
        ResponseEntity<Void> protectedCall = restTemplate.exchange(
                "/api/clients/" + nonexistentClient, HttpMethod.GET, bearerRequest, Void.class);
        assertThat(protectedCall.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Logout is 204 idempotent.
        ResponseEntity<Void> logout = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST, bearerRequest, Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Re-using the revoked token must be rejected.
        ResponseEntity<Void> afterLogout = restTemplate.exchange(
                "/api/clients/" + nonexistentClient, HttpMethod.GET, bearerRequest, Void.class);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void malformedBearerTokenIsRejected() {
        HttpEntity<Void> bearerRequest = new HttpEntity<>(bearerHeaders("not-a-real-jwt"));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/clients/" + UUID.randomUUID(), HttpMethod.GET, bearerRequest, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void missingBearerHeaderIsRejected() {
        ResponseEntity<Void> response = restTemplate.getForEntity(
                "/api/clients/" + UUID.randomUUID(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
