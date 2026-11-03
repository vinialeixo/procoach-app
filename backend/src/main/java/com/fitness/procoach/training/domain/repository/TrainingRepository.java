package com.fitness.procoach.training.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingStatus;

public interface TrainingRepository {

	Training save(Training training);

	Optional<Training> findById(UUID id);

	List<Training> findByPeriodizationId(UUID periodizationId);

	List<Training> findByAthleteId(UUID athleteId);

	List<Training> findByCoachId(UUID coachId);

	List<Training> findByAthleteIdAndStatus(UUID athleteId, TrainingStatus status);

	List<Training> findByPeriodizationIdAndDeletedFalse(UUID periodizationId);

	/** Sessões (não removidas) de uma versão do plano. */
	List<Training> findByPlanIdAndDeletedFalse(UUID planId);

}
