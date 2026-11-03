package com.fitness.procoach.coach.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório Spring Data da entidade JPA de Coach.
 */
public interface CoachJpaRepository extends JpaRepository<CoachJpaEntity, UUID> {

	Optional<CoachJpaEntity> findByUserId(UUID userId);

	boolean existsByUserId(UUID userId);
}
