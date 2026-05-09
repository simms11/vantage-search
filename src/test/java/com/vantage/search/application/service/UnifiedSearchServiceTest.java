package com.vantage.search.application.service;
import com.vantage.search.application.port.out.AiSearchPort;
import com.vantage.search.application.port.out.ClientPersistencePort;
import com.vantage.search.application.service.UnifiedSearchService;
import com.vantage.search.domain.model.Client;
import com.vantage.search.domain.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class UnifiedSearchServiceTest {

    @Mock
    private ClientPersistencePort clientPersistencePort;

    @Mock
    private AiSearchPort aiSearchPort;

    @InjectMocks
    private UnifiedSearchService unifiedSearchService;

    @Test
    void search_shouldReturnEmptyList_whenQueryIsBlank() {
        // GIVEN
        String query = "   ";

        // WHEN
        List<SearchResult> results = unifiedSearchService.search(query);

        // THEN
        assertThat(results).isEmpty();
        verifyNoInteractions(clientPersistencePort, aiSearchPort);
    }

    @Test
    void search_shouldCombineAndNormaliseScoresUsingRRF() {
        // GIVEN
        String query = "Vantage";

        Client mockClient = new Client(
                UUID.randomUUID(), "John", "Doe", "john@vantage.com",
                null, Set.of()
        );

        SearchResult.DocumentMatch mockDoc = new SearchResult.DocumentMatch(
                UUID.randomUUID(), UUID.randomUUID(), "Title", "Summary",
                OffsetDateTime.now(), 0.85, "AI Match"
        );

        when(clientPersistencePort.findClientsByFuzzySearch(eq(query.toLowerCase()), anyInt())).thenReturn(List.of(mockClient));
        when(aiSearchPort.findSimilarDocuments(eq(query), anyInt())).thenReturn(List.of(mockDoc));

        // WHEN
        List<SearchResult> results = unifiedSearchService.search(query);

        // THEN
        assertThat(results).hasSize(2);

        double expectedRrfScore = 1.0 / 61.0;
        assertThat(results.get(0).score()).isCloseTo(expectedRrfScore, within(0.0001));
        assertThat(results.get(1).score()).isCloseTo(expectedRrfScore, within(0.0001));

        assertThat(results.get(0).explanation()).contains("(Normalised via RRF)");
        assertThat(results.get(1).explanation()).contains("(Normalised via RRF)");
    }

    @Test
    void search_shouldGracefullyDegrade_whenAiSearchFails() {
        // GIVEN
        String query = "test";
        Client mockClient = new Client(
                UUID.randomUUID(), "Jane", "Smith", "jane@test.com",
                null, Set.of()
        );

        when(clientPersistencePort.findClientsByFuzzySearch(eq(query.toLowerCase()), anyInt())).thenReturn(List.of(mockClient));
        when(aiSearchPort.findSimilarDocuments(eq(query), anyInt())).thenThrow(new RuntimeException("LLM Timeout"));

        // WHEN
        List<SearchResult> results = unifiedSearchService.search(query);

        // THEN
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isInstanceOf(SearchResult.ClientMatch.class);

        double expectedRrfScore = 1.0 / 61.0;
        assertThat(results.get(0).score()).isCloseTo(expectedRrfScore, within(0.0001));
    }

    @Test
    void search_shouldGracefullyDegrade_whenDbSearchFails() {
        // GIVEN
        String query = "test";
        SearchResult.DocumentMatch mockDoc = new SearchResult.DocumentMatch(
                UUID.randomUUID(), UUID.randomUUID(), "Doc", "Sum",
                OffsetDateTime.now(), 0.50, "AI Match"
        );

        when(clientPersistencePort.findClientsByFuzzySearch(eq(query.toLowerCase()), anyInt())).thenThrow(new RuntimeException("DB Connection Lost"));
        when(aiSearchPort.findSimilarDocuments(eq(query), anyInt())).thenReturn(List.of(mockDoc));

        // WHEN
        List<SearchResult> results = unifiedSearchService.search(query);

        // THEN
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isInstanceOf(SearchResult.DocumentMatch.class);

        double expectedRrfScore = 1.0 / 61.0;
        assertThat(results.get(0).score()).isCloseTo(expectedRrfScore, within(0.0001));
    }
}
