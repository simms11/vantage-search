package com.vantage.search.application.service;

import com.vantage.search.application.dto.request.ClientRequest;
import com.vantage.search.application.dto.request.DocumentRequest;
import com.vantage.search.application.port.in.ClientUseCase;
import com.vantage.search.application.port.out.ClientPersistencePort;
import com.vantage.search.application.port.out.DocumentPersistencePort;
import com.vantage.search.domain.event.DocumentCreatedEvent;
import com.vantage.search.domain.model.Client;
import com.vantage.search.domain.model.SearchResult;
import com.vantage.search.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientManagementService implements ClientUseCase {

    private final ClientPersistencePort clientPersistencePort;
    private final DocumentPersistencePort documentPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SearchResult.ClientMatch createClient(ClientRequest request) {
        Client client = new Client(null, request.firstName(), request.lastName(),
                request.email(), request.description(), request.socialLinks());

        Client saved = clientPersistencePort.save(client);

        return toClientMatch(saved, 1.0, "Newly created client");
    }

    @Override
    @Transactional
    public SearchResult.DocumentMatch createDocument(UUID clientId, DocumentRequest request) {
        if (!clientPersistencePort.clientExists(clientId)) {
            throw new ResourceNotFoundException("Client not found: " + clientId);
        }

        UUID docId = documentPersistencePort.save(clientId, request.title(), request.content());

        eventPublisher.publishEvent(new DocumentCreatedEvent(docId, clientId, request.title(), request.content()));

        return new SearchResult.DocumentMatch(
                docId, clientId, request.title(),
                "Processing summary...", OffsetDateTime.now(),
                1.0, "Document successfully staged for AI indexing"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult.ClientMatch getClient(UUID clientId) {
        Client client = clientPersistencePort.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));
        return toClientMatch(client, 1.0, "Client record");
    }

    @Override
    @Transactional
    public void deleteClient(UUID clientId) {
        if (!clientPersistencePort.clientExists(clientId)) {
            throw new ResourceNotFoundException("Client not found: " + clientId);
        }
        clientPersistencePort.deleteById(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchResult.DocumentMatch> getClientDocuments(UUID clientId) {
        if (!clientPersistencePort.clientExists(clientId)) {
            throw new ResourceNotFoundException("Client not found: " + clientId);
        }
        return documentPersistencePort.findByClientId(clientId);
    }

    private SearchResult.ClientMatch toClientMatch(Client client, double score, String explanation) {
        return new SearchResult.ClientMatch(
                client.id(), client.firstName(), client.lastName(),
                client.email(), client.description(), client.socialLinks(),
                score, explanation
        );
    }
}
