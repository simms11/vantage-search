package com.vantage.search_api.infrastructure.persistence.repository;

import com.vantage.search_api.infrastructure.persistence.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    List<DocumentEntity> findByClientId(UUID clientId);
}
