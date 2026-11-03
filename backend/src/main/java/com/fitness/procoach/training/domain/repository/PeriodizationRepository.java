package com.fitness.procoach.training.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Periodization;

public interface PeriodizationRepository {

	Periodization save(Periodization periodization);

	Optional<Periodization> findById(UUID id);

	List<Periodization> findByAthleteId(UUID athleteId);

	List<Periodization> findByCoachId(UUID coachId);

	List<Periodization> findByAthleteIdAndDeletedFalse(UUID athleteId);

}
