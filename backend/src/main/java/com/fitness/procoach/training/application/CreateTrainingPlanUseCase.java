package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
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

	private final CoachApplicationService coachService;

	private final AthleteAccessService athleteAccessService;

	private final AuditService auditService;

	public CreateTrainingPlanUseCase(TrainingPlanRepository trainingPlanRepository,
			PeriodizationRepository periodizationRepository, ApplicationEventPublisher eventPublisher,
			CoachApplicationService coachService, AthleteAccessService athleteAccessService,
			AuditService auditService) {
		this.trainingPlanRepository = trainingPlanRepository;
		this.periodizationRepository = periodizationRepository;
		this.eventPublisher = eventPublisher;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
		this.auditService = auditService;
	}

	public TrainingPlan execute(UUID athleteId, UUID userId, UUID periodizationId, String name, String description) {
		// T-058: o coachId nunca vem do request — é resolvido server-side a partir
		// do usuário autenticado, e o atleta-alvo precisa pertencer a esse coach.
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		athleteAccessService.requireOwnedByCoach(athleteId, coachId);
		Periodization periodization = this.periodizationRepository.findById(periodizationId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("Periodization not found: " + periodizationId));
		// A periodização precisa ser do mesmo atleta — senão um coach vincularia treino/plano
		// à periodização de outro coach. 404 para não vazar a existência.
		if (!periodization.getAthleteId().equals(athleteId)) {
			throw new TrainingResourceNotFoundException("Periodization not found: " + periodizationId);
		}

		TrainingPlan plan = TrainingPlan.create(athleteId, coachId, periodization.getId(), name, description);
		TrainingPlan saved = this.trainingPlanRepository.save(plan);

		this.eventPublisher.publishEvent(new TrainingPlanCreatedEvent(saved.getId(), saved.getPeriodizationId(),
				saved.getAthleteId(), saved.getCoachId(), saved.getName(), saved.getVersion(),
				saved.getPreviousVersionId()));

		this.auditService.record("TrainingPlan", saved.getId(), saved.getAthleteId(), "TRAINING_PLAN_CREATED", userId,
				AuditData.of("name", saved.getName(), "version", saved.getVersion()));
		return saved;
	}

}
