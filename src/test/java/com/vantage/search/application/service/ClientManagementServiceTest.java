package com.vantage.search.application.service;

import com.vantage.search.application.dto.request.DocumentRequest;
import com.vantage.search.application.port.out.ClientPersistencePort;
import com.vantage.search.application.port.out.DocumentPersistencePort;
import com.vantage.search.domain.event.DocumentCreatedEvent;
import com.vantage.search.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientManagementServiceTest {

    @Mock
    private ClientPersistencePort clientPersistencePort;

    @Mock
    private DocumentPersistencePort documentPersistencePort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ClientManagementService service;

    @Test
    void createDocument_shouldPersistAndPublishEvent_whenClientExists() {
        UUID clientId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        DocumentRequest request = new DocumentRequest("Utility Bill", "Address proof content");

        when(clientPersistencePort.clientExists(clientId)).thenReturn(true);
        when(documentPersistencePort.save(eq(clientId), anyString(), anyString())).thenReturn(docId);

        service.createDocument(clientId, request);

        verify(documentPersistencePort).save(clientId, "Utility Bill", "Address proof content");
        verify(eventPublisher).publishEvent(any(DocumentCreatedEvent.class));
    }

    @Test
    void createDocument_shouldThrowResourceNotFoundException_whenClientDoesNotExist() {
        UUID unknownClientId = UUID.randomUUID();
        DocumentRequest request = new DocumentRequest("Title", "Content");

        when(clientPersistencePort.clientExists(unknownClientId)).thenReturn(false);

        assertThatThrownBy(() -> service.createDocument(unknownClientId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownClientId.toString());

        verifyNoInteractions(documentPersistencePort, eventPublisher);
    }
}
