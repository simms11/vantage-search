package com.vantage.search_api.application.port.out;

import com.vantage.search_api.domain.model.SearchResult;
import java.util.List;

public interface AiSearchPort {
    List<SearchResult.DocumentMatch> findSimilarDocuments(String query, int topK);
}
