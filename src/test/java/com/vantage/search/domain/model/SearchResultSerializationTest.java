package com.vantage.search.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SearchResultSerializationTest {

    @Test
    void shouldRecreateRecordWithUpdatedScoreAndExplanation() {
        SearchResult.ClientMatch original = new SearchResult.ClientMatch(
                UUID.randomUUID(), "John", "Doe", "john@vantage.com",
                "Desc", Set.of(), 0.85, "Exact email match"
        );

        SearchResult updated = original.withUpdatedScore(0.016);

        assertThat(updated.score()).isEqualTo(0.016);
        assertThat(updated.explanation()).isEqualTo("Exact email match (Normalised via RRF)");
        assertThat(original.score()).isEqualTo(0.85);
    }

    @Test
    void shouldRecreateDocumentMatchWithUpdatedScoreAndExplanation() {
        SearchResult.DocumentMatch original = new SearchResult.DocumentMatch(
                UUID.randomUUID(), UUID.randomUUID(), "Title", "Summary",
                OffsetDateTime.now(), 0.90, "AI match"
        );

        SearchResult updated = original.withUpdatedScore(0.012);

        assertThat(updated.score()).isEqualTo(0.012);
        assertThat(updated.explanation()).isEqualTo("AI match (Normalised via RRF)");
        assertThat(original.score()).isEqualTo(0.90);
    }
}
