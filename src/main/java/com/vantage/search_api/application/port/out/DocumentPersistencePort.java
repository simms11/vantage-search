package com.vantage.search_api.application.port.out;

import com.vantage.search_api.domain.model.SearchResult;

import java.util.List;
import java.util.UUID;

public interface DocumentPersistencePort {

    UUID save(UUID clientId, String title, String content);

    void updateSummary(UUID documentId, String summary);

    List<SearchResult.DocumentMatch> findByClientId(UUID clientId);
}
