package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.training.domain.event.TrainingCreatedEvent;
import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.model.TrainingStatus;
import com.fitness.procoach.training.domain.repository.PeriodizationRepository;
import com.fitness.procoach.training.domain.repository.TrainingPlanRepository;
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

	private final CoachApplicationService coachService;

	private final AthleteAccessService athleteAccessService;

	private final AuditService auditService;

	private final TrainingPlanRepository trainingPlanRepository;

	public CreateTrainingUseCase(TrainingRepository trainingRepository, PeriodizationRepository periodizationRepository,
			ApplicationEventPublisher eventPublisher, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService, AuditService auditService,
			TrainingPlanRepository trainingPlanRepository) {
		this.trainingRepository = trainingRepository;
		this.periodizationRepository = periodizationRepository;
		this.eventPublisher = eventPublisher;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
		this.auditService = auditService;
		this.trainingPlanRepository = trainingPlanRepository;
	}

	public Training execute(UUID periodizationId, UUID planId, UUID athleteId, UUID userId, String name,
			String description, int weekNumber, int dayOfWeek) {
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

		// A sessão pertence a uma versão do plano (T-067): o plano precisa ser deste atleta e desta
		// periodização, senão 404. Versão finalizada não recebe sessões novas.
		TrainingPlan plan = this.trainingPlanRepository.findById(planId)
			.filter(p -> !p.isDeleted())
			.orElseThrow(() -> new TrainingResourceNotFoundException("TrainingPlan not found: " + planId));
		if (!plan.getAthleteId().equals(athleteId) || !plan.getPeriodizationId().equals(periodization.getId())) {
			throw new TrainingResourceNotFoundException("TrainingPlan not found: " + planId);
		}
		if (plan.getStatus() == TrainingStatus.FINISHED) {
			throw new IllegalStateException("Plan version is finished; create a new version to add sessions");
		}

		Training training = Training.create(periodization.getId(), plan.getId(), athleteId, coachId, name, description,
				weekNumber,
				dayOfWeek);
		Training saved = this.trainingRepository.save(training);

		this.eventPublisher.publishEvent(
				new TrainingCreatedEvent(saved.getId(), saved.getPeriodizationId(), saved.getAthleteId(),
						saved.getCoachId(), saved.getName()));

		this.auditService.record("Training", saved.getId(), saved.getAthleteId(), "TRAINING_CREATED", userId,
				AuditData.of("name", saved.getName()));
		return saved;
	}

}
