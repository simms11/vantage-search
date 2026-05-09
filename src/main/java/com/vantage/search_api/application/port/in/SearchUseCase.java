package com.vantage.search_api.application.port.in;

import com.vantage.search_api.domain.model.SearchResult;
import java.util.List;

public interface SearchUseCase {

    List<SearchResult> search(String query, int page, int size);

    default List<SearchResult> search(String query) {
        return search(query, 0, 20);
    }
}