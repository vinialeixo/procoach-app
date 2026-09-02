package com.fitness.procoach.training.application;

import java.time.LocalDate;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.repository.PeriodizationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreatePeriodizationUseCase {

	private final PeriodizationRepository periodizationRepository;

	public CreatePeriodizationUseCase(PeriodizationRepository periodizationRepository) {
		this.periodizationRepository = periodizationRepository;
	}

	public Periodization execute(UUID athleteId, UUID coachId, String name, String description, LocalDate startDate,
			LocalDate endDate) {
		Periodization periodization = Periodization.create(athleteId, coachId, name, description, startDate, endDate);
		return this.periodizationRepository.save(periodization);
	}

}
