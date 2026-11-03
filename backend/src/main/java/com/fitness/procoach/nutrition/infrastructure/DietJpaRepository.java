package com.fitness.procoach.nutrition.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório Spring Data da entidade JPA de Diet.
 */
public interface DietJpaRepository extends JpaRepository<DietJpaEntity, UUID> {

	List<DietJpaEntity> findByAthleteIdOrderByVersionAsc(UUID athleteId);

	Optional<DietJpaEntity> findFirstByAthleteIdOrderByVersionDesc(UUID athleteId);
}
