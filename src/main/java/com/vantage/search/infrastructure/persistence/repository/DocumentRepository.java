package com.vantage.search.infrastructure.persistence.repository;

import com.vantage.search.infrastructure.persistence.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    List<DocumentEntity> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    @Modifying
    @Query("UPDATE DocumentEntity d SET d.summary = :summary WHERE d.id = :id")
    int updateSummaryById(@Param("id") UUID id, @Param("summary") String summary);
}
