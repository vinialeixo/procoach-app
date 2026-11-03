package com.fitness.procoach.assessment.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência do agregado Assessment (framework-agnostic).
 */
public interface AssessmentRepository {

	Assessment save(Assessment assessment);

	Optional<Assessment> findById(AssessmentId id);

	/**
	 * Lista as avaliações de um atleta em ordem cronológica (US-007).
	 */
	List<Assessment> findByAthleteIdOrderByAssessmentDateAsc(UUID athleteId);
}
