package com.vantage.search.infrastructure.persistence.adapter;

import com.vantage.search.application.port.out.DocumentPersistencePort;
import com.vantage.search.domain.model.Document;
import com.vantage.search.infrastructure.persistence.entity.DocumentEntity;
import com.vantage.search.infrastructure.persistence.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPersistenceAdapter implements DocumentPersistencePort {

    private final DocumentRepository repository;

    @Override
    @Transactional
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
    @Transactional
    public void updateSummary(UUID documentId, String summary) {
        int updated = repository.updateSummaryById(documentId, summary);
        if (updated == 0) {
            log.warn("Document {} not found during summary update; it may have been deleted before AI processing completed.", documentId);
        }
    }

    @Override
    public List<Document> findByClientId(UUID clientId) {
        return repository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(doc -> new Document(
                        doc.getId(),
                        doc.getClientId(),
                        doc.getTitle(),
                        doc.getSummary(),
                        doc.getCreatedAt()
                ))
                .toList();
    }
}
