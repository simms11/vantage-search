package com.vantage.search.infrastructure.web.controller;

import com.vantage.search.application.dto.request.ClientRequest;
import com.vantage.search.application.dto.request.DocumentRequest;
import com.vantage.search.application.port.in.ClientUseCase;
import com.vantage.search.domain.model.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Client Management", description = "Operations for managing client lifecycles and associated records")
public class ClientManagementController {

    private final ClientUseCase clientUseCase;

    @Operation(summary = "Register a new client", description = "Initialises a new wealth management client profile.")
    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public SearchResult.ClientMatch createClient(@Valid @RequestBody ClientRequest request) {
        return clientUseCase.createClient(request);
    }

    @Operation(summary = "Get a client by ID")
    @GetMapping("/clients/{id}")
    public SearchResult.ClientMatch getClient(@PathVariable UUID id) {
        return clientUseCase.getClient(id);
    }

    @Operation(summary = "Delete a client", description = "Permanently removes the client and all associated documents.")
    @DeleteMapping("/clients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(@PathVariable UUID id) {
        clientUseCase.deleteClient(id);
    }

    @Operation(summary = "List documents for a client")
    @GetMapping("/clients/{id}/documents")
    public List<SearchResult.DocumentMatch> getClientDocuments(@PathVariable UUID id) {
        return clientUseCase.getClientDocuments(id);
    }

    @Operation(summary = "Attach document to client", description = "Persists a document and triggers background AI summarisation and indexing.")
    @PostMapping("/clients/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public SearchResult.DocumentMatch createDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentRequest request) {
        return clientUseCase.createDocument(id, request);
    }
}
