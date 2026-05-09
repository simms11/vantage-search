package com.vantage.search_api.infrastructure.security;

import com.vantage.search_api.infrastructure.persistence.entity.TokenBlacklistEntity;
import com.vantage.search_api.infrastructure.persistence.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository repository;

    @Transactional
    public void revoke(String jti, OffsetDateTime expiresAt) {
        if (!repository.existsByJti(jti)) {
            repository.save(TokenBlacklistEntity.builder()
                    .id(UUID.randomUUID())
                    .jti(jti)
                    .expiresAt(expiresAt)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return repository.existsByJti(jti);
    }

    @Transactional
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        repository.deleteByExpiresAtBefore(OffsetDateTime.now());
        log.debug("Purged expired token blacklist entries.");
    }
}
