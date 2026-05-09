package com.vantage.search.application.port.out;

import com.vantage.search.domain.model.Client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientPersistencePort {

    Client save(Client client);

    boolean clientExists(UUID clientId);

    Optional<Client> findById(UUID clientId);

    void deleteById(UUID clientId);

    List<Client> findClientsByFuzzySearch(String query, int limit);
}
