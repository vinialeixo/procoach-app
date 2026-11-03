package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
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

	private final CoachApplicationService coachService;

	private final AthleteAccessService athleteAccessService;

	public CreateTrainingUseCase(TrainingRepository trainingRepository, PeriodizationRepository periodizationRepository,
			ApplicationEventPublisher eventPublisher, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService) {
		this.trainingRepository = trainingRepository;
		this.periodizationRepository = periodizationRepository;
		this.eventPublisher = eventPublisher;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	public Training execute(UUID periodizationId, UUID athleteId, UUID userId, String name, String description,
			int weekNumber, int dayOfWeek) {
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

		Training training = Training.create(periodization.getId(), athleteId, coachId, name, description, weekNumber,
				dayOfWeek);
		Training saved = this.trainingRepository.save(training);

		this.eventPublisher.publishEvent(
				new TrainingCreatedEvent(saved.getId(), saved.getPeriodizationId(), saved.getAthleteId(),
						saved.getCoachId(), saved.getName()));

		return saved;
	}

}
