package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.training.domain.event.TrainingPlanCreatedEvent;
import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.repository.PeriodizationRepository;
import com.fitness.procoach.training.domain.repository.TrainingPlanRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateTrainingPlanUseCase {

	private final TrainingPlanRepository trainingPlanRepository;

	private final PeriodizationRepository periodizationRepository;

	private final ApplicationEventPublisher eventPublisher;

	public CreateTrainingPlanUseCase(TrainingPlanRepository trainingPlanRepository,
			PeriodizationRepository periodizationRepository, ApplicationEventPublisher eventPublisher) {
		this.trainingPlanRepository = trainingPlanRepository;
		this.periodizationRepository = periodizationRepository;
		this.eventPublisher = eventPublisher;
	}

	public TrainingPlan execute(UUID athleteId, UUID coachId, UUID periodizationId, String name, String description) {
		Periodization periodization = this.periodizationRepository.findById(periodizationId)
			.orElseThrow(() -> new IllegalArgumentException("Periodization not found: " + periodizationId));

		TrainingPlan plan = TrainingPlan.create(athleteId, coachId, periodization.getId(), name, description);
		TrainingPlan saved = this.trainingPlanRepository.save(plan);

		this.eventPublisher.publishEvent(new TrainingPlanCreatedEvent(saved.getId(), saved.getPeriodizationId(),
				saved.getAthleteId(), saved.getCoachId(), saved.getName(), saved.getVersion(),
				saved.getPreviousVersionId()));

		return saved;
	}

}
