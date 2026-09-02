package com.fitness.procoach.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TrainingPlanSpringDataRepository extends JpaRepository<TrainingPlanJpaEntity, UUID> {

	List<TrainingPlanJpaEntity> findByAthleteId(UUID athleteId);

	List<TrainingPlanJpaEntity> findByCoachId(UUID coachId);

	List<TrainingPlanJpaEntity> findByPeriodizationId(UUID periodizationId);

	List<TrainingPlanJpaEntity> findByAthleteIdAndStatus(UUID athleteId, TrainingStatus status);

	List<TrainingPlanJpaEntity> findByPreviousVersionId(UUID previousVersionId);

	@Query("SELECT tp FROM TrainingPlanJpaEntity tp WHERE tp.athleteId = :athleteId AND tp.deleted = false "
			+ "ORDER BY tp.version DESC LIMIT 1")
	Optional<TrainingPlanJpaEntity> findLatestVersionByAthleteId(@Param("athleteId") UUID athleteId);

}
