package com.vantage.search_api.infrastructure.persistence.repository;

import com.vantage.search_api.infrastructure.persistence.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

    @SuppressWarnings("SqlResolve")
    @Query(value = """
        SELECT * FROM clients
        WHERE similarity(first_name || ' ' || last_name || ' ' || email || ' ' || COALESCE(description, ''), :q) > 0.15
           OR email ILIKE CONCAT('%', :q, '%')
        ORDER BY similarity(first_name || ' ' || last_name || ' ' || email, :q) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ClientEntity> searchClients(@Param("q") String query, @Param("limit") int limit);
}