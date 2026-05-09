package com.vantage.search.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.search.config.RateLimitProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    /**
     * Atomic INCR + (conditional) EXPIRE.
     *
     * <p>Combining the two commands into a single Lua script closes the boundary-burst window
     * (where INCR succeeds but EXPIRE never lands, leaving a key without TTL) and prevents the
     * "double window" that fixed-window non-atomic implementations leak around expiry.
     */
    private static final byte[] RATE_LIMIT_SCRIPT = ("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """).getBytes(StandardCharsets.UTF_8);

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private Counter failOpenCounter;

    @PostConstruct
    void initMetrics() {
        // Fail-open is silent by design (preserve availability when Redis is
        // down) but a sustained increment means the rate limiter is no longer
        // enforcing; alert on a non-zero rate. Optional in case this filter
        // runs in a context without metrics (slice tests, etc.).
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry != null) {
            failOpenCounter = Counter.builder("rate.limit.fail.open")
                    .description("Requests allowed because the rate-limit Redis check failed.")
                    .register(registry);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health and metrics probes are infrastructure traffic (k8s liveness,
        // Prometheus scrapes, load balancers). They should never compete with
        // user requests for the per-IP budget.
        //
        // Use the servlet path (already URI-decoded and normalised) rather than
        // the raw request URI; otherwise a path like "/actuator/../api/clients"
        // would match this prefix and bypass the rate limiter.
        String path = request.getServletPath();
        return path != null && path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String ip = resolveClientIp(request);
        String key = "rate-limit:" + ip;

        try {
            Long count = incrementAndExpire(key, properties.refillDurationSeconds());
            if (count != null && count > properties.capacity()) {
                rejectWithTooManyRequests(response);
                return;
            }
        } catch (Exception e) {
            // Redis unavailable; fail open to preserve API availability.
            if (failOpenCounter != null) {
                failOpenCounter.increment();
            }
            log.warn("Rate limit check failed for {}: {}. Allowing request.", ip, e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private Long incrementAndExpire(String key, long ttlSeconds) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] ttlBytes = Long.toString(ttlSeconds).getBytes(StandardCharsets.UTF_8);
        return redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.scriptingCommands().eval(
                        RATE_LIMIT_SCRIPT,
                        ReturnType.INTEGER,
                        1,
                        keyBytes,
                        ttlBytes));
    }

    private void rejectWithTooManyRequests(HttpServletResponse response) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please slow down and try again.");
        pd.setTitle("Too Many Requests");
        pd.setType(URI.create("https://api.vantage.com/errors/rate-limit-exceeded"));
        pd.setProperty("timestamp", Instant.now());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), pd);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (properties.trustProxy()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
