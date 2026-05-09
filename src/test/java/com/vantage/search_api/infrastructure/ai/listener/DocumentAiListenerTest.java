package com.vantage.search_api.infrastructure.ai.listener;

import com.vantage.search_api.application.port.out.AiSummaryPort;
import com.vantage.search_api.application.port.out.DocumentPersistencePort;
import com.vantage.search_api.domain.event.DocumentCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentAiListenerTest {

    @Mock VectorStore vectorStore;
    @Mock AiSummaryPort aiSummaryPort;
    @Mock DocumentPersistencePort documentPersistencePort;

    @InjectMocks DocumentAiListener listener;

    @Test
    void shouldStoreDocumentWithCorrectIdAndMetadata() {
        UUID docId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        DocumentCreatedEvent event = new DocumentCreatedEvent(docId, clientId, "Tax Report 2024", "Full content body.");

        when(aiSummaryPort.generateBulletSummary(anyString())).thenReturn("- Point 1\n- Point 2\n- Point 3");

        listener.handleDocumentCreated(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        Document aiDoc = captor.getValue().get(0);
        assertThat(aiDoc.getId()).isEqualTo(docId.toString());
        assertThat(aiDoc.getMetadata()).containsEntry("clientId", clientId.toString());
        assertThat(aiDoc.getMetadata()).containsEntry("title", "Tax Report 2024");
        assertThat(aiDoc.getMetadata().get("summary")).asString().contains("Point 1");
        assertThat(aiDoc.getMetadata().get("createdAt")).isNotNull();
    }

    @Test
    void shouldPersistSummaryToDB() {
        UUID docId = UUID.randomUUID();
        DocumentCreatedEvent event = new DocumentCreatedEvent(docId, UUID.randomUUID(), "Title", "Content");

        when(aiSummaryPort.generateBulletSummary(anyString())).thenReturn("Summary text");

        listener.handleDocumentCreated(event);

        verify(documentPersistencePort).updateSummary(docId, "Summary text");
    }

    @Test
    void shouldNotPropagateException_whenAiProcessingFails() {
        DocumentCreatedEvent event = new DocumentCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "Title", "Content");

        when(aiSummaryPort.generateBulletSummary(anyString())).thenThrow(new RuntimeException("LLM unavailable"));

        assertThatNoException().isThrownBy(() -> listener.handleDocumentCreated(event));
        verifyNoInteractions(vectorStore);
    }
}
