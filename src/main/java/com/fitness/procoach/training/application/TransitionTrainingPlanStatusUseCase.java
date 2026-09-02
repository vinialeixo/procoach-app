package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.training.domain.event.TrainingStatusChangedEvent;
import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.model.TrainingStatus;
import com.fitness.procoach.training.domain.repository.TrainingPlanRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransitionTrainingPlanStatusUseCase {

	private final TrainingPlanRepository trainingPlanRepository;

	private final ApplicationEventPublisher eventPublisher;

	public TransitionTrainingPlanStatusUseCase(TrainingPlanRepository trainingPlanRepository,
			ApplicationEventPublisher eventPublisher) {
		this.trainingPlanRepository = trainingPlanRepository;
		this.eventPublisher = eventPublisher;
	}

	public TrainingPlan execute(UUID planId, TrainingStatus targetStatus) {
		TrainingPlan plan = this.trainingPlanRepository.findById(planId)
			.orElseThrow(() -> new IllegalArgumentException("TrainingPlan not found: " + planId));

		TrainingStatus previousStatus = plan.getStatus();
		plan.transitionTo(targetStatus);
		TrainingPlan saved = this.trainingPlanRepository.save(plan);

		this.eventPublisher.publishEvent(TrainingStatusChangedEvent.forTrainingPlan(saved.getId(),
				saved.getAthleteId(), saved.getCoachId(), previousStatus, saved.getStatus()));

		return saved;
	}

}
