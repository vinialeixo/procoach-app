package com.fitness.procoach.training.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.model.TrainingStatus;

public interface TrainingPlanRepository {

	TrainingPlan save(TrainingPlan trainingPlan);

	Optional<TrainingPlan> findById(UUID id);

	List<TrainingPlan> findByAthleteId(UUID athleteId);

	List<TrainingPlan> findByCoachId(UUID coachId);

	List<TrainingPlan> findByPeriodizationId(UUID periodizationId);

	List<TrainingPlan> findByAthleteIdAndStatus(UUID athleteId, TrainingStatus status);

	Optional<TrainingPlan> findLatestVersionByAthleteId(UUID athleteId);

	List<TrainingPlan> findByPreviousVersionId(UUID previousVersionId);

}
