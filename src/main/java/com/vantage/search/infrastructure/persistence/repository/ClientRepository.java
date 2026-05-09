package com.vantage.search.infrastructure.persistence.repository;

import com.vantage.search.infrastructure.persistence.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

    /**
     * The WHERE and ORDER BY use the same expression as the GIN trigram index
     * (changelog 005-add-trigram-index-for-search) so Postgres can plan the
     * filter against the index rather than a sequential scan + sort.
     *
     * <p>The {@code LEAST(:limit, 1000)} clamp is defence-in-depth against an
     * internal caller that bypasses {@code UnifiedSearchService}'s
     * {@code MAX_CANDIDATE_LIMIT}; without it a misbehaving caller could ask
     * for an arbitrarily large result set.
     */
    @SuppressWarnings("SqlResolve")
    @Query(value = """
        SELECT * FROM clients
        WHERE similarity(first_name || ' ' || last_name || ' ' || email || ' ' || COALESCE(description, ''), :q) > 0.15
           OR email ILIKE CONCAT('%', :q, '%')
        ORDER BY similarity(first_name || ' ' || last_name || ' ' || email || ' ' || COALESCE(description, ''), :q) DESC
        LIMIT LEAST(:limit, 1000)
        """, nativeQuery = true)
    List<ClientEntity> searchClients(@Param("q") String query, @Param("limit") int limit);
}