package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.training.domain.event.TrainingPlanCreatedEvent;
import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.repository.TrainingPlanRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateTrainingPlanVersionUseCase {

	private final TrainingPlanRepository trainingPlanRepository;

	private final ApplicationEventPublisher eventPublisher;

	private final TrainingOwnershipGuard ownershipGuard;

	public CreateTrainingPlanVersionUseCase(TrainingPlanRepository trainingPlanRepository,
			ApplicationEventPublisher eventPublisher, TrainingOwnershipGuard ownershipGuard) {
		this.trainingPlanRepository = trainingPlanRepository;
		this.eventPublisher = eventPublisher;
		this.ownershipGuard = ownershipGuard;
	}

	public TrainingPlan execute(UUID currentPlanId, UUID userId, String newName, String newDescription) {
		TrainingPlan current = this.trainingPlanRepository.findById(currentPlanId)
			.orElseThrow(() -> new IllegalArgumentException("TrainingPlan not found: " + currentPlanId));

		// T-058 (anti-IDOR): o plano precisa pertencer a um atleta da carteira
		// do coach autenticado — plano de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(current.getAthleteId(), userId);

		TrainingPlan newVersion = current.createNewVersion(newName, newDescription);
		TrainingPlan saved = this.trainingPlanRepository.save(newVersion);

		this.eventPublisher.publishEvent(new TrainingPlanCreatedEvent(saved.getId(), saved.getPeriodizationId(),
				saved.getAthleteId(), saved.getCoachId(), saved.getName(), saved.getVersion(),
				saved.getPreviousVersionId()));

		return saved;
	}

}
