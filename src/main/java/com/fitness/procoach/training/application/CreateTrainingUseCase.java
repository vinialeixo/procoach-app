package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.training.domain.event.TrainingCreatedEvent;
import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.repository.PeriodizationRepository;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateTrainingUseCase {

	private final TrainingRepository trainingRepository;

	private final PeriodizationRepository periodizationRepository;

	private final ApplicationEventPublisher eventPublisher;

	public CreateTrainingUseCase(TrainingRepository trainingRepository, PeriodizationRepository periodizationRepository,
			ApplicationEventPublisher eventPublisher) {
		this.trainingRepository = trainingRepository;
		this.periodizationRepository = periodizationRepository;
		this.eventPublisher = eventPublisher;
	}

	public Training execute(UUID periodizationId, UUID athleteId, UUID coachId, String name, String description,
			int weekNumber, int dayOfWeek) {
		Periodization periodization = this.periodizationRepository.findById(periodizationId)
			.orElseThrow(() -> new IllegalArgumentException("Periodization not found: " + periodizationId));

		Training training = Training.create(periodization.getId(), athleteId, coachId, name, description, weekNumber,
				dayOfWeek);
		Training saved = this.trainingRepository.save(training);

		this.eventPublisher.publishEvent(
				new TrainingCreatedEvent(saved.getId(), saved.getPeriodizationId(), saved.getAthleteId(),
						saved.getCoachId(), saved.getName()));

		return saved;
	}

}
