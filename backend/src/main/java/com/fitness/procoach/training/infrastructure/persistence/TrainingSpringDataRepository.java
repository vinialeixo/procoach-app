package com.fitness.procoach.training.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingStatus;

import org.springframework.data.jpa.repository.JpaRepository;

interface TrainingSpringDataRepository extends JpaRepository<TrainingJpaEntity, UUID> {

	List<TrainingJpaEntity> findByPeriodizationId(UUID periodizationId);

	List<TrainingJpaEntity> findByAthleteId(UUID athleteId);

	List<TrainingJpaEntity> findByCoachId(UUID coachId);

	List<TrainingJpaEntity> findByAthleteIdAndStatus(UUID athleteId, TrainingStatus status);

	List<TrainingJpaEntity> findByPeriodizationIdAndDeletedFalse(UUID periodizationId);

}
