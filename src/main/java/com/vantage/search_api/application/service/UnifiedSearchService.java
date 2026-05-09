package com.vantage.search_api.application.service;

import com.vantage.search_api.application.port.in.SearchUseCase;
import com.vantage.search_api.application.port.out.AiSearchPort;
import com.vantage.search_api.application.port.out.ClientPersistencePort;
import com.vantage.search_api.domain.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Orchestrates hybrid search by merging lexical results (PostgreSQL) with
 * semantic matches (AI). Utilises Reciprocal Rank Fusion (RRF) to normalise
 * different scoring distributions into a single, ranked result set.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedSearchService implements SearchUseCase {

    private final ClientPersistencePort clientPersistencePort;
    private final AiSearchPort aiSearchPort;

    private static final int RRF_K_CONSTANT = 60;
    private static final int MAX_CANDIDATE_LIMIT = 200;

    @Override
    public List<SearchResult> search(String query, int page, int size) {
        if (query == null || query.trim().isBlank()) {
            return Collections.emptyList();
        }

        String normalisedQuery = query.trim().toLowerCase();

        // Fetch enough candidates from each source to serve this page and a buffer
        int candidateLimit = Math.min((page + 1) * size + 20, MAX_CANDIDATE_LIMIT);

        List<SearchResult> clientMatches = new ArrayList<>();
        List<SearchResult> documentMatches = new ArrayList<>();

        try {
            clientMatches.addAll(clientPersistencePort.findClientsByFuzzySearch(normalisedQuery, candidateLimit));
        } catch (Exception e) {
            log.error("Relational search failed for query: {}. Reason: {}", query, e.getMessage());
        }

        try {
            documentMatches.addAll(aiSearchPort.findSimilarDocuments(query, candidateLimit));
        } catch (Exception e) {
            log.warn("AI Vector search unavailable. Returning database matches only. Error: {}", e.getMessage());
        }

        List<SearchResult> all = applyReciprocalRankFusion(clientMatches, documentMatches);
        int from = page * size;
        if (from >= all.size()) return Collections.emptyList();
        return all.subList(from, Math.min(from + size, all.size()));
    }

    private List<SearchResult> applyReciprocalRankFusion(
            List<SearchResult> clientMatches,
            List<SearchResult> documentMatches) {

        Map<UUID, Double> rrfScores = new LinkedHashMap<>();
        Map<UUID, SearchResult> entityMap = new HashMap<>();

        processRankedResults(clientMatches, rrfScores, entityMap);
        processRankedResults(documentMatches, rrfScores, entityMap);

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .map(entry -> entityMap.get(entry.getKey()).withUpdatedScore(entry.getValue()))
                .toList();
    }

    private void processRankedResults(
            List<? extends SearchResult> matches,
            Map<UUID, Double> rrfScores,
            Map<UUID, SearchResult> entityMap) {

        for (int i = 0; i < matches.size(); i++) {
            SearchResult match = matches.get(i);
            UUID id = match.id();
            double currentRankScore = 1.0 / (RRF_K_CONSTANT + i + 1);
            rrfScores.put(id, rrfScores.getOrDefault(id, 0.0) + currentRankScore);
            entityMap.putIfAbsent(id, match);
        }
    }
}
