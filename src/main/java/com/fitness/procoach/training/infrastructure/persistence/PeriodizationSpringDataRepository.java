package com.fitness.procoach.training.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PeriodizationSpringDataRepository extends JpaRepository<PeriodizationJpaEntity, UUID> {

	List<PeriodizationJpaEntity> findByAthleteId(UUID athleteId);

	List<PeriodizationJpaEntity> findByCoachId(UUID coachId);

	List<PeriodizationJpaEntity> findByAthleteIdAndDeletedFalse(UUID athleteId);

}
