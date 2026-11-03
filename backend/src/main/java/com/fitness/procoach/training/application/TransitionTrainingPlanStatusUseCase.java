package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.training.domain.event.TrainingStatusChangedEvent;
import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.model.TrainingStatus;
import com.fitness.procoach.training.domain.repository.TrainingPlanRepository;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

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

	private final TrainingRepository trainingRepository;

	private final TrainingNotifier notifier;

	public TransitionTrainingPlanStatusUseCase(TrainingPlanRepository trainingPlanRepository,
			ApplicationEventPublisher eventPublisher, TrainingOwnershipGuard ownershipGuard,
			AuditService auditService, TrainingRepository trainingRepository, TrainingNotifier notifier) {
		this.trainingPlanRepository = trainingPlanRepository;
		this.eventPublisher = eventPublisher;
		this.ownershipGuard = ownershipGuard;
		this.auditService = auditService;
		this.trainingRepository = trainingRepository;
		this.notifier = notifier;
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
		if (saved.getStatus() == TrainingStatus.ACTIVE) {
			publish(saved, userId);
		}
		this.eventPublisher.publishEvent(TrainingStatusChangedEvent.forTrainingPlan(saved.getId(),
				saved.getAthleteId(), saved.getCoachId(), previousStatus, saved.getStatus()));

		return saved;
	}

	/**
	 * Plano ativado (T-067): as sessões dele passam a valer para o atleta (rascunho e revisão viram
	 * ACTIVE), as versões anteriores ainda ativas são encerradas junto com as suas sessões — viram
	 * histórico, intactas — e o atleta é avisado.
	 */
	private void publish(TrainingPlan plan, UUID userId) {
		for (Training session : this.trainingRepository.findByPlanIdAndDeletedFalse(plan.getId())) {
			TrainingStatus before = session.getStatus();
			if (session.publish()) {
				Training saved = this.trainingRepository.save(session);
				this.auditService.record("Training", saved.getId(), saved.getAthleteId(), "TRAINING_STATUS_CHANGED",
						userId, AuditData.of("from", before.name(), "to", saved.getStatus().name(), "cause",
								"plan_published"));
			}
		}
		UUID previousId = plan.getPreviousVersionId();
		while (previousId != null) {
			TrainingPlan previous = this.trainingPlanRepository.findById(previousId).orElse(null);
			if (previous == null) {
				break;
			}
			if (!previous.isDeleted() && previous.getStatus() == TrainingStatus.ACTIVE) {
				previous.transitionTo(TrainingStatus.FINISHED);
				TrainingPlan finished = this.trainingPlanRepository.save(previous);
				this.auditService.record("TrainingPlan", finished.getId(), finished.getAthleteId(),
						"TRAINING_PLAN_STATUS_CHANGED", userId, AuditData.of("from", "ACTIVE", "to", "FINISHED",
								"cause", "new_version_published"));
				for (Training session : this.trainingRepository.findByPlanIdAndDeletedFalse(finished.getId())) {
					if (session.finishIfActive()) {
						Training saved = this.trainingRepository.save(session);
						this.auditService.record("Training", saved.getId(), saved.getAthleteId(),
								"TRAINING_STATUS_CHANGED", userId, AuditData.of("from", "ACTIVE", "to", "FINISHED",
										"cause", "new_version_published"));
					}
				}
			}
			previousId = previous.getPreviousVersionId();
		}
		this.notifier.planPublished(plan, plan.getVersion() > 1
				? "Seu coach publicou uma nova versão do plano de treino \"" + plan.getName() + "\" (versão "
						+ plan.getVersion() + ")."
				: "Seu plano de treino \"" + plan.getName() + "\" está disponível.");
	}
}
