package com.vantage.search.infrastructure.persistence.adapter;

import com.vantage.search.application.port.out.DocumentPersistencePort;
import com.vantage.search.domain.model.SearchResult;
import com.vantage.search.infrastructure.persistence.entity.DocumentEntity;
import com.vantage.search.infrastructure.persistence.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPersistenceAdapter implements DocumentPersistencePort {

    private final DocumentRepository repository;

    @Override
    public UUID save(UUID clientId, String title, String content) {
        DocumentEntity entity = DocumentEntity.builder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .title(title)
                .content(content)
                .summary(null)
                .build();

        return repository.save(entity).getId();
    }

    @Override
    public void updateSummary(UUID documentId, String summary) {
        repository.findById(documentId).ifPresentOrElse(
                doc -> {
                    doc.setSummary(summary);
                    repository.save(doc);
                },
                () -> log.warn("Document {} not found during summary update; it may have been deleted before AI processing completed.", documentId)
        );
    }

    @Override
    public List<SearchResult.DocumentMatch> findByClientId(UUID clientId) {
        return repository.findByClientId(clientId).stream()
                .map(doc -> new SearchResult.DocumentMatch(
                        doc.getId(),
                        doc.getClientId(),
                        doc.getTitle(),
                        doc.getSummary(),
                        doc.getCreatedAt(),
                        1.0,
                        "Document belonging to client"
                ))
                .toList();
    }
}
