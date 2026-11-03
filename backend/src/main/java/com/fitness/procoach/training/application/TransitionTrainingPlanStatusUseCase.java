package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.training.domain.event.TrainingStatusChangedEvent;
import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
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

	private final TrainingOwnershipGuard ownershipGuard;

	private final AuditService auditService;

	public TransitionTrainingPlanStatusUseCase(TrainingPlanRepository trainingPlanRepository,
			ApplicationEventPublisher eventPublisher, TrainingOwnershipGuard ownershipGuard,
			AuditService auditService) {
		this.trainingPlanRepository = trainingPlanRepository;
		this.eventPublisher = eventPublisher;
		this.ownershipGuard = ownershipGuard;
		this.auditService = auditService;
	}

	public TrainingPlan execute(UUID planId, UUID userId, TrainingStatus targetStatus) {
		TrainingPlan plan = this.trainingPlanRepository.findById(planId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("TrainingPlan not found: " + planId));

		// T-058 (anti-IDOR): o plano precisa pertencer a um atleta da carteira
		// do coach autenticado — plano de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(plan.getAthleteId(), userId);

		TrainingStatus previousStatus = plan.getStatus();
		plan.transitionTo(targetStatus);
		TrainingPlan saved = this.trainingPlanRepository.save(plan);

		this.auditService.record("TrainingPlan", saved.getId(), saved.getAthleteId(), "TRAINING_PLAN_STATUS_CHANGED",
				userId, AuditData.of("from", previousStatus.name(), "to", saved.getStatus().name()));
		this.eventPublisher.publishEvent(TrainingStatusChangedEvent.forTrainingPlan(saved.getId(),
				saved.getAthleteId(), saved.getCoachId(), previousStatus, saved.getStatus()));

		return saved;
	}

}
