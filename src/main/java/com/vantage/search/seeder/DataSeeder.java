package com.vantage.search.seeder;

import com.vantage.search.infrastructure.persistence.entity.ClientEntity;
import com.vantage.search.infrastructure.persistence.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ClientRepository clientRepository;
    private final VectorStore vectorStore;

    @Override
    @Transactional
    public void run(String... args) {

        if (clientRepository.count() == 0) {
            log.info("[SEEDER] Database is empty. Adding seed clients...");

            clientRepository.save(ClientEntity.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@vantage.com")
                    .description("High net worth advisor based in London")
                    .build());

            clientRepository.save(ClientEntity.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@vantage.com")
                    .description("Expert in tax optimisation and wealth structuring")
                    .build());
        }

        List<Document> existingDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query("wealth").topK(1).similarityThreshold(0.0).build()
        );

        if (existingDocs.isEmpty()) {
            log.info("[SEEDER] Vector store is empty. Seeding sample documents...");

            UUID seedClientId = clientRepository.findAll().stream()
                    .findFirst()
                    .map(ClientEntity::getId)
                    .orElse(null);

            UUID docId1 = UUID.randomUUID();
            UUID docId2 = UUID.randomUUID();
            String now = OffsetDateTime.now().toString();

            Map<String, Object> meta1 = seedClientId != null
                    ? Map.of("docId", docId1.toString(), "clientId", seedClientId.toString(),
                             "title", "Q3 Tax Strategy", "summary", "Q3 Tax Strategy report.", "createdAt", now)
                    : Map.of("docId", docId1.toString(), "title", "Q3 Tax Strategy");

            Map<String, Object> meta2 = seedClientId != null
                    ? Map.of("docId", docId2.toString(), "clientId", seedClientId.toString(),
                             "title", "Address Verification", "summary", "Proof of address document.", "createdAt", now)
                    : Map.of("docId", docId2.toString(), "title", "Address Verification");

            vectorStore.add(List.of(
                    new Document(docId1.toString(),
                            "Q3 Tax Strategy. Key changes include adjustments to capital gains tax.", meta1),
                    new Document(docId2.toString(),
                            "Proof of Address: Recent utility bill for the property at 123 Vantage Lane.", meta2)
            ));

            log.info("[SEEDER] Seed documents added.");
        }
    }
}
