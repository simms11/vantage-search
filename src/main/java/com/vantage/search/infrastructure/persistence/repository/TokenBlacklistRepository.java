package com.vantage.search.infrastructure.persistence.repository;

import com.vantage.search.infrastructure.persistence.entity.TokenBlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklistEntity, UUID> {
    boolean existsByJti(String jti);
    void deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
