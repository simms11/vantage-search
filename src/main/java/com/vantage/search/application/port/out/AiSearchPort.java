package com.vantage.search.application.port.out;

import com.vantage.search.domain.model.SearchResult;
import java.util.List;

public interface AiSearchPort {
    List<SearchResult.DocumentMatch> findSimilarDocuments(String query, int topK);
}
