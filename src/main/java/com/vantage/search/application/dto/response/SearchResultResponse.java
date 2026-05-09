package com.vantage.search.application.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.vantage.search.domain.model.SearchResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Wire-format response for search hits. Keeps Jackson polymorphism configuration
 * out of the domain so domain types stay framework-free.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SearchResultResponse.ClientMatch.class, name = "client"),
        @JsonSubTypes.Type(value = SearchResultResponse.DocumentMatch.class, name = "document")
})
public sealed interface SearchResultResponse permits SearchResultResponse.ClientMatch, SearchResultResponse.DocumentMatch {

    record ClientMatch(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String description,
            List<String> socialLinks,
            Double score,
            String explanation
    ) implements SearchResultResponse {
        public static ClientMatch from(SearchResult.ClientMatch m) {
            return new ClientMatch(m.id(), m.firstName(), m.lastName(), m.email(),
                    m.description(), m.socialLinks(), m.score(), m.explanation());
        }
    }

    record DocumentMatch(
            UUID id,
            UUID clientId,
            String title,
            String summary,
            OffsetDateTime createdAt,
            Double score,
            String explanation
    ) implements SearchResultResponse {
        public static DocumentMatch from(SearchResult.DocumentMatch m) {
            return new DocumentMatch(m.id(), m.clientId(), m.title(), m.summary(),
                    m.createdAt(), m.score(), m.explanation());
        }
    }

    static SearchResultResponse from(SearchResult result) {
        return switch (result) {
            case SearchResult.ClientMatch cm -> ClientMatch.from(cm);
            case SearchResult.DocumentMatch dm -> DocumentMatch.from(dm);
        };
    }
}
