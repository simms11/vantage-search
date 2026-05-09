package com.vantage.search_api.infrastructure.ai.adapter;

import com.vantage.search_api.application.port.out.AiSearchPort;
import com.vantage.search_api.domain.model.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiSearchAdapter implements AiSearchPort {

    private final VectorStore vectorStore;

    @Override
    public List<SearchResult.DocumentMatch> findSimilarDocuments(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .similarityThreshold(0.5)
                .topK(topK)
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(doc -> {
                    UUID docId = UUID.fromString(doc.getId());

                    String clientIdStr = (String) doc.getMetadata().get("clientId");
                    UUID clientId = clientIdStr != null ? UUID.fromString(clientIdStr) : null;

                    String title = (String) doc.getMetadata().getOrDefault("title", "Untitled Document");
                    String summary = (String) doc.getMetadata().getOrDefault("summary", doc.getText());

                    String createdAtStr = (String) doc.getMetadata().get("createdAt");
                    OffsetDateTime createdAt = createdAtStr != null ? OffsetDateTime.parse(createdAtStr) : null;

                    // pgvector returns cosine distance (0 = identical, 1 = orthogonal); convert to similarity
                    Object distanceObj = doc.getMetadata().getOrDefault("distance", 1.0);
                    double distance = (distanceObj instanceof Number n) ? n.doubleValue() : 1.0;
                    double score = 1.0 - distance;

                    return new SearchResult.DocumentMatch(
                            docId,
                            clientId,
                            title,
                            summary,
                            createdAt,
                            score,
                            "Semantic match via AI vector embeddings"
                    );
                })
                .toList();
    }
}
