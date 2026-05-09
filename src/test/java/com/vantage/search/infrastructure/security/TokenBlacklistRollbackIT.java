package com.vantage.search.infrastructure.security;

import com.vantage.search.exception.ServiceUnavailableException;
import com.vantage.search.infrastructure.persistence.repository.TokenBlacklistRepository;
import com.vantage.search.support.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Pins the atomicity contract documented on {@link TokenBlacklistService#revoke}:
 * if Redis fails after the JPA insert is staged, the @Transactional proxy must
 * roll back so the durable store does not drift from the cache.
 *
 * <p>If this test fails, treat it as a load-bearing change to the auth flow:
 * the service no longer guarantees that revoke is all-or-nothing.
 */
class TokenBlacklistRollbackIT extends BaseIT {

    @SpyBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private TokenBlacklistRepository repository;

    @Test
    void redisWriteFailureRollsBackTheDatabaseInsert() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> throwingOps = mock(ValueOperations.class);
        doThrow(new QueryTimeoutException("simulated Redis timeout"))
                .when(throwingOps).set(anyString(), anyString(), any(Duration.class));
        doReturn(throwingOps).when(redisTemplate).opsForValue();

        String jti = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(10);

        assertThatThrownBy(() -> tokenBlacklistService.revoke(jti, expiresAt))
                .isInstanceOf(ServiceUnavailableException.class);

        assertThat(repository.existsByJti(jti))
                .as("DB insert must be rolled back when the Redis cache write fails")
                .isFalse();
    }
}
