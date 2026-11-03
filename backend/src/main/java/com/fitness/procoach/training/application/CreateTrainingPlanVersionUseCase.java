package com.fitness.procoach.training.application;

import java.util.List;
import java.util.UUID;

import com.fitness.procoach.training.domain.event.TrainingPlanCreatedEvent;
import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.repository.TrainingPlanRepository;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateTrainingPlanVersionUseCase {

	private final TrainingPlanRepository trainingPlanRepository;

	private final ApplicationEventPublisher eventPublisher;

	private final TrainingOwnershipGuard ownershipGuard;

	private final AuditService auditService;

	private final TrainingRepository trainingRepository;

	public CreateTrainingPlanVersionUseCase(TrainingPlanRepository trainingPlanRepository,
			ApplicationEventPublisher eventPublisher, TrainingOwnershipGuard ownershipGuard,
			AuditService auditService, TrainingRepository trainingRepository) {
		this.trainingPlanRepository = trainingPlanRepository;
		this.eventPublisher = eventPublisher;
		this.ownershipGuard = ownershipGuard;
		this.auditService = auditService;
		this.trainingRepository = trainingRepository;
	}

	public TrainingPlan execute(UUID currentPlanId, UUID userId, String newName, String newDescription) {
		TrainingPlan current = this.trainingPlanRepository.findById(currentPlanId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("TrainingPlan not found: " + currentPlanId));

		// T-058 (anti-IDOR): o plano precisa pertencer a um atleta da carteira
		// do coach autenticado — plano de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(current.getAthleteId(), userId);

		TrainingPlan newVersion = current.createNewVersion(newName, newDescription);
		TrainingPlan saved = this.trainingPlanRepository.save(newVersion);

		// T-067: a nova versão nasce com uma cópia das sessões da versão de origem (em rascunho); as
		// sessões da origem continuam intactas, como o atleta as viu.
		List<Training> originSessions = this.trainingRepository.findByPlanIdAndDeletedFalse(current.getId());
		for (Training session : originSessions) {
			this.trainingRepository.save(session.copyForNewPlanVersion(saved.getId()));
		}
		this.auditService.record("TrainingPlan", saved.getId(), saved.getAthleteId(), "TRAINING_PLAN_VERSIONED", userId,
				AuditData.of("version", saved.getVersion(), "previousVersionId", saved.getPreviousVersionId(),
						"copiedTrainings", originSessions.size()));
		this.eventPublisher.publishEvent(new TrainingPlanCreatedEvent(saved.getId(), saved.getPeriodizationId(),
				saved.getAthleteId(), saved.getCoachId(), saved.getName(), saved.getVersion(),
				saved.getPreviousVersionId()));

		return saved;
	}

}
