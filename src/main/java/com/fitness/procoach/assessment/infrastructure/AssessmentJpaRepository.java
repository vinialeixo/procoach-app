package com.fitness.procoach.assessment.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório Spring Data da entidade JPA de Assessment.
 */
public interface AssessmentJpaRepository extends JpaRepository<AssessmentJpaEntity, UUID> {

	List<AssessmentJpaEntity> findByAthleteIdOrderByAssessmentDateAsc(UUID athleteId);
}
