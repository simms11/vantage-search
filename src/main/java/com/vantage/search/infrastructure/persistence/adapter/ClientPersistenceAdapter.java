package com.vantage.search.infrastructure.persistence.adapter;

import com.vantage.search.application.port.out.ClientPersistencePort;
import com.vantage.search.domain.model.Client;
import com.vantage.search.domain.model.SearchResult;
import com.vantage.search.infrastructure.persistence.entity.ClientEntity;
import com.vantage.search.infrastructure.persistence.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientPersistenceAdapter implements ClientPersistencePort {

    private final ClientRepository repository;

    @Override
    public Client save(Client client) {
        ClientEntity entity = ClientEntity.builder()
                .firstName(client.firstName())
                .lastName(client.lastName())
                .email(client.email())
                .description(client.description())
                .socialLinks(client.socialLinks())
                .build();
        return toClient(repository.save(entity));
    }

    @Override
    public boolean clientExists(UUID clientId) {
        return repository.existsById(clientId);
    }

    @Override
    public Optional<Client> findById(UUID clientId) {
        return repository.findById(clientId).map(this::toClient);
    }

    @Override
    public void deleteById(UUID clientId) {
        repository.deleteById(clientId);
    }

    @Override
    public List<SearchResult.ClientMatch> findClientsByFuzzySearch(String query, int limit) {
        return repository.searchClients(query, limit).stream()
                .map(entity -> new SearchResult.ClientMatch(
                        entity.getId(),
                        entity.getFirstName(),
                        entity.getLastName(),
                        entity.getEmail(),
                        entity.getDescription(),
                        entity.getSocialLinks(),
                        1.0,
                        "Trigram Match: Name or Email domain matches query"
                ))
                .toList();
    }

    private Client toClient(ClientEntity entity) {
        return new Client(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getDescription(),
                entity.getSocialLinks()
        );
    }
}
