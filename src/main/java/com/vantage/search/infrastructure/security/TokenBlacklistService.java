package com.vantage.search.infrastructure.security;

import com.vantage.search.exception.ServiceUnavailableException;
import com.vantage.search.infrastructure.persistence.entity.TokenBlacklistEntity;
import com.vantage.search.infrastructure.persistence.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Redis-primary blacklist with the database as durable audit and recovery storage.
 *
 * <p>Read path: every authenticated request hits Redis only. The DB is consulted at
 * startup (warm-up) and never on the request hot path.
 *
 * <p>Write path: revoke writes both Redis (with TTL = remaining token lifetime) and
 * the DB row (idempotent insert). The DB exists so revocations survive a Redis flush.
 *
 * <p>Failure mode: if Redis is unreachable on a lookup, fail open (treat as not
 * revoked). Aligns with the rate-limiter's fail-open posture and the project's
 * graceful-degradation goal: a Redis outage shouldn't lock everyone out.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "jwt:revoked:";

    private final TokenBlacklistRepository repository;
    private final StringRedisTemplate redisTemplate;

    /**
     * Revoke a token by jti.
     *
     * <p>Atomicity contract: the DB row and the Redis cache entry are written
     * together or neither is. Because this method is {@code @Transactional},
     * the JPA insert is staged but not committed until the proxy returns
     * normally; throwing {@link ServiceUnavailableException} after a Redis
     * write failure causes Spring to roll back the staged insert. On the next
     * client retry the DB is empty again, the insert succeeds, and Redis is
     * re-attempted, so revoke is fully idempotent across partial failures.
     *
     * <p>If you change this method's exception types, propagation, or rollback
     * rules, also update {@code TokenBlacklistRollbackIT} which pins this
     * contract.
     */
    @Transactional
    public void revoke(String jti, OffsetDateTime expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        if (!repository.existsByJti(jti)) {
            repository.save(TokenBlacklistEntity.builder()
                    .id(UUID.randomUUID())
                    .jti(jti)
                    .expiresAt(expiresAt)
                    .build());
        }
        // Redis is the read path: a write failure here means the token would
        // continue to authenticate. Surface 503 so the client retries instead
        // of receiving a misleading 204.
        try {
            cacheRevocation(jti, expiresAt, OffsetDateTime.now());
        } catch (DataAccessException ex) {
            throw new ServiceUnavailableException(
                    "Token revocation cache unavailable; please retry.", ex);
        }
    }

    /**
     * Repopulate Redis from the durable store once the application is fully
     * started, so a Redis flush doesn't resurrect previously-revoked tokens
     * until they expire. Listening on {@link ApplicationReadyEvent} (rather
     * than {@code @PostConstruct}) ensures the transactional proxy is wired
     * before this method runs and that the readiness signal has fired.
     * Swallows Redis errors so application start is not blocked on Redis.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void warmCacheFromDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        int restored = 0;
        int failed = 0;
        for (TokenBlacklistEntity entity : repository.findByExpiresAtAfter(now)) {
            try {
                cacheRevocation(entity.getJti(), entity.getExpiresAt(), now);
                restored++;
            } catch (DataAccessException ex) {
                failed++;
                log.warn("Redis warm-up write failed for jti={}: {}", entity.getJti(), ex.getMessage());
            }
        }
        if (failed > 0) {
            log.warn("Token blacklist cache warm-up partial: restored={}, failed={}. "
                    + "Failed jtis remain in DB and will be retried at next restart.",
                    restored, failed);
        } else if (restored > 0) {
            log.info("Warmed token blacklist cache with {} unexpired entries.", restored);
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
        } catch (Exception e) {
            log.warn("Redis blacklist lookup failed for jti={}: {}. Failing open.", jti, e.getMessage());
            return false;
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        repository.deleteByExpiresAtBefore(OffsetDateTime.now());
        log.debug("Purged expired token blacklist entries.");
    }

    private void cacheRevocation(String jti, OffsetDateTime expiresAt, OffsetDateTime now) {
        long seconds = Duration.between(now, expiresAt).getSeconds();
        if (seconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(seconds));
    }
}
