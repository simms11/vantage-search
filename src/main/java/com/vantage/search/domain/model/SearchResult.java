package com.vantage.search.domain.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Unified API contract for polymorphic search results.
 * Ensures a predictable JSON schema for API consumers.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SearchResult.ClientMatch.class, name = "client"),
        @JsonSubTypes.Type(value = SearchResult.DocumentMatch.class, name = "document")
})
public sealed interface SearchResult permits SearchResult.ClientMatch, SearchResult.DocumentMatch {
    UUID id();
    Double score();
    String explanation();
    SearchResult withUpdatedScore(Double newScore);
    record ClientMatch(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String description,
            List<String> socialLinks,
            Double score,
            String explanation
    ) implements SearchResult {
        @Override
        public SearchResult withUpdatedScore(Double newScore) {
            return new ClientMatch(
                    id, firstName, lastName, email, description, socialLinks,
                    newScore, explanation + " (Normalised via RRF)"
            );
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
    ) implements SearchResult {
        @Override
        public SearchResult withUpdatedScore(Double newScore) {
            return new DocumentMatch(
                    id, clientId, title, summary, createdAt,
                    newScore, explanation + " (Normalised via RRF)"
            );
        }
    }
}