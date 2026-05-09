package com.vantage.search_api.application.service;

import com.vantage.search_api.support.BaseIT;
import com.vantage.search_api.application.port.in.SearchUseCase;
import com.vantage.search_api.domain.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SearchStrategyIT extends BaseIT {

    @Autowired
    private SearchUseCase searchUseCase;

    @MockBean
    private VectorStore vectorStore;

    @Test
    void shouldReturnDocumentMatchWithCorrectMetadataFromVectorStore() {
        UUID docId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        String createdAt = OffsetDateTime.now().toString();

        Document doc = new Document(
                docId.toString(),
                "Tax Report 2024 Bullet summary of tax changes.",
                Map.of(
                        "docId", docId.toString(),
                        "clientId", clientId.toString(),
                        "title", "Tax Report 2024",
                        "summary", "Bullet summary of tax changes.",
                        "createdAt", createdAt,
                        "distance", 0.2f
                )
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        List<SearchResult> results = searchUseCase.search("tax strategy");

        assertThat(results).isNotEmpty();
        SearchResult.DocumentMatch match = (SearchResult.DocumentMatch) results.get(0);
        assertThat(match.id()).isEqualTo(docId);
        assertThat(match.clientId()).isEqualTo(clientId);
        assertThat(match.title()).isEqualTo("Tax Report 2024");
        assertThat(match.summary()).isEqualTo("Bullet summary of tax changes.");
        assertThat(match.explanation())
                .contains("Semantic match")
                .contains("Normalised via RRF");
        assertThat(match.score()).isLessThan(0.1);
    }
}
