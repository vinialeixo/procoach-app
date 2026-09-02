package com.procoach.core.athlete.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório Spring Data da entidade JPA de Athlete.
 */
public interface AthleteJpaRepository extends JpaRepository<AthleteJpaEntity, UUID> {
}
