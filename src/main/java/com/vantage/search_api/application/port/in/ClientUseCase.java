package com.vantage.search_api.application.port.in;

import com.vantage.search_api.application.dto.request.ClientRequest;
import com.vantage.search_api.application.dto.request.DocumentRequest;
import com.vantage.search_api.domain.model.SearchResult;

import java.util.List;
import java.util.UUID;

public interface ClientUseCase {

    SearchResult.ClientMatch createClient(ClientRequest request);

    SearchResult.DocumentMatch createDocument(UUID clientId, DocumentRequest request);

    SearchResult.ClientMatch getClient(UUID clientId);

    void deleteClient(UUID clientId);

    List<SearchResult.DocumentMatch> getClientDocuments(UUID clientId);
}
