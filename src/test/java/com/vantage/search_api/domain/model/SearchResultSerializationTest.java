package com.vantage.search_api.domain.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.search_api.domain.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
@JsonTest
public class SearchResultSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeClientMatchProperly() throws Exception {
        // GIVEN
        SearchResult client = new SearchResult.ClientMatch(
                UUID.randomUUID(), "John", "Doe", "john@vantage.com", "Desc", List.of("link"), 1.0, "Explanation"
        );

        // WHEN
        String json = objectMapper.writeValueAsString(client);

        // THEN
        assertThat(json).contains("\"firstName\":\"John\"");
        assertThat(json).contains("\"lastName\":\"Doe\"");
        assertThat(json).contains("\"type\":\"client\""); // Verifies the Jackson @JsonTypeInfo logic
    }

    @Test
    void shouldRecreateRecordWithUpdatedScoreAndExplanation() {
        // GIVEN
        SearchResult.ClientMatch original = new SearchResult.ClientMatch(
                UUID.randomUUID(), "John", "Doe", "john@vantage.com",
                "Desc", List.of(), 0.85, "Exact email match"
        );

        // WHEN
        SearchResult updated = (SearchResult.ClientMatch) original.withUpdatedScore(0.016);

        // THEN
        assertThat(updated.score()).isEqualTo(0.016);
        assertThat(updated.explanation()).isEqualTo("Exact email match (Normalised via RRF)");
        assertThat(original.score()).isEqualTo(0.85);
    }

    @Test
    void shouldRecreateDocumentMatchWithUpdatedScoreAndExplanation() {
        // GIVEN
        SearchResult.DocumentMatch original = new SearchResult.DocumentMatch(
                UUID.randomUUID(), UUID.randomUUID(), "Title", "Summary",
                OffsetDateTime.now(), 0.90, "AI match"
        );

        // WHEN
        SearchResult updated = original.withUpdatedScore(0.012);

        // THEN
        assertThat(updated.score()).isEqualTo(0.012);
        assertThat(updated.explanation()).isEqualTo("AI match (Normalised via RRF)");
        assertThat(original.score()).isEqualTo(0.90);
    }
}