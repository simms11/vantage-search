package com.vantage.search.application.port.out;

import com.vantage.search.domain.model.Client;
import com.vantage.search.domain.model.SearchResult;

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
