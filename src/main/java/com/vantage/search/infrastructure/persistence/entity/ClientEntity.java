package com.vantage.search.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Stored as a Set so duplicate links from input are silently coalesced and
     * the (client_id, link) composite PK in client_social_links is not violated.
     */
    @ElementCollection
    @CollectionTable(name = "client_social_links", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "link")
    @BatchSize(size = 50)
    private Set<String> socialLinks;
}
