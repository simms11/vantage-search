package com.vantage.search.application.port.out;

import com.vantage.search.domain.model.SearchResult;

import java.util.List;
import java.util.UUID;

public interface DocumentPersistencePort {

    UUID save(UUID clientId, String title, String content);

    void updateSummary(UUID documentId, String summary);

    List<SearchResult.DocumentMatch> findByClientId(UUID clientId);
}
