package com.vantage.search.infrastructure.ai.listener;

import com.vantage.search.application.port.out.AiSummaryPort;
import com.vantage.search.application.port.out.DocumentPersistencePort;
import com.vantage.search.domain.event.DocumentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentAiListener {

    private static final int MAX_ATTEMPTS = 3;

    private final VectorStore vectorStore;
    private final AiSummaryPort aiSummaryPort;
    private final DocumentPersistencePort documentPersistencePort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentCreated(DocumentCreatedEvent event) {
        log.info("Processing AI tasks for document: {}", event.documentId());

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                processAiTasks(event);
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("AI processing attempt {}/{} failed for document {}: {}",
                        attempt, MAX_ATTEMPTS, event.documentId(), e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("AI processing permanently failed for document {} after {} attempts: {}",
                event.documentId(), MAX_ATTEMPTS, lastException != null ? lastException.getMessage() : "interrupted");
    }

    private void processAiTasks(DocumentCreatedEvent event) {
        String summary = aiSummaryPort.generateBulletSummary(event.content());

        documentPersistencePort.updateSummary(event.documentId(), summary);

        Document aiDoc = new Document(
                event.documentId().toString(),
                event.title() + " " + summary,
                Map.of(
                        "docId", event.documentId().toString(),
                        "clientId", event.clientId().toString(),
                        "title", event.title(),
                        "summary", summary,
                        "createdAt", OffsetDateTime.now().toString()
                )
        );
        vectorStore.add(List.of(aiDoc));

        log.info("AI processing complete for document: {}", event.documentId());
    }
}
