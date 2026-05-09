package com.vantage.search.infrastructure.security;

import com.vantage.search.infrastructure.persistence.entity.TokenBlacklistEntity;
import com.vantage.search.infrastructure.persistence.repository.TokenBlacklistRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * Repopulate Redis from the durable store on startup so a Redis flush
     * doesn't resurrect previously-revoked tokens until they expire.
     */
    @PostConstruct
    @Transactional(readOnly = true)
    public void warmCacheFromDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        int restored = 0;
        for (TokenBlacklistEntity entity : repository.findByExpiresAtAfter(now)) {
            if (cacheRevocation(entity.getJti(), entity.getExpiresAt(), now)) {
                restored++;
            }
        }
        if (restored > 0) {
            log.info("Warmed token blacklist cache with {} unexpired entries.", restored);
        }
    }

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
        cacheRevocation(jti, expiresAt, OffsetDateTime.now());
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

    private boolean cacheRevocation(String jti, OffsetDateTime expiresAt, OffsetDateTime now) {
        long seconds = Duration.between(now, expiresAt).getSeconds();
        if (seconds <= 0) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(seconds));
            return true;
        } catch (Exception e) {
            log.warn("Redis blacklist cache write failed for jti={}: {}", jti, e.getMessage());
            return false;
        }
    }
}
