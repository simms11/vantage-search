package com.vantage.search_api.application.port.out;

import com.vantage.search_api.domain.model.Client;
import com.vantage.search_api.domain.model.SearchResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientPersistencePort {

    Client save(Client client);

    boolean clientExists(UUID clientId);

    Optional<Client> findById(UUID clientId);

    void deleteById(UUID clientId);

    List<SearchResult.ClientMatch> findClientsByFuzzySearch(String query, int limit);
}
