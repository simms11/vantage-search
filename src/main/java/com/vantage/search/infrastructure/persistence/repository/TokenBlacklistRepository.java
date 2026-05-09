package com.vantage.search.infrastructure.persistence.repository;

import com.vantage.search.infrastructure.persistence.entity.TokenBlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklistEntity, UUID> {

    boolean existsByJti(String jti);

    void deleteByExpiresAtBefore(OffsetDateTime cutoff);

    List<TokenBlacklistEntity> findByExpiresAtAfter(OffsetDateTime now);

    /**
     * Idempotent insert. Two concurrent logouts of the same token previously
     * raced through {@code existsByJti}/{@code save} and the loser hit a
     * unique-constraint violation; the {@code ON CONFLICT DO NOTHING} clause
     * collapses both calls to a single inserted row without an exception.
     */
    @Modifying
    @Query(value = """
        INSERT INTO token_blacklist (id, jti, expires_at, created_at)
        VALUES (:id, :jti, :expiresAt, :createdAt)
        ON CONFLICT (jti) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("jti") String jti,
            @Param("expiresAt") OffsetDateTime expiresAt,
            @Param("createdAt") OffsetDateTime createdAt);
}
