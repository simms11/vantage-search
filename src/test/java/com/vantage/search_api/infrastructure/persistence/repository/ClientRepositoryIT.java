package com.vantage.search_api.infrastructure.persistence.repository;

import com.vantage.search_api.support.BaseIT;
import com.vantage.search_api.infrastructure.persistence.entity.ClientEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientRepositoryIT extends BaseIT {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE documents, client_social_links, clients CASCADE");
    }

    @Test
    void shouldFindClientsUsingFuzzyMatching() {
        ClientEntity client = ClientEntity.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@vantage.com")
                .build();
        clientRepository.save(client);

        List<ClientEntity> results = clientRepository.searchClients("vantage wealth", 50);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("john.doe@vantage.com");
    }

    @Test
    void shouldFindClientEvenWithTypos() {
        ClientEntity client = ClientEntity.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@vantage.com")
                .build();
        clientRepository.save(client);

        List<ClientEntity> results = clientRepository.searchClients("vntage wealth", 50);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFirstName()).isEqualTo("Jane");
    }
}
