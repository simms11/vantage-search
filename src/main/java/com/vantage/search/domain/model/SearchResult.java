package com.vantage.search.domain.model;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Unified domain model for polymorphic search results.
 * Wire-format concerns (JSON shape, polymorphism discriminator) live in the
 * {@code application.dto.response.SearchResultResponse} family.
 */
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
            Set<String> socialLinks,
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
