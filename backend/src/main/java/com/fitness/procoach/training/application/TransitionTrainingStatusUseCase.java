package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.training.domain.event.TrainingStatusChangedEvent;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingStatus;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransitionTrainingStatusUseCase {

	private final TrainingRepository trainingRepository;

	private final ApplicationEventPublisher eventPublisher;

	private final TrainingOwnershipGuard ownershipGuard;

	public TransitionTrainingStatusUseCase(TrainingRepository trainingRepository,
			ApplicationEventPublisher eventPublisher, TrainingOwnershipGuard ownershipGuard) {
		this.trainingRepository = trainingRepository;
		this.eventPublisher = eventPublisher;
		this.ownershipGuard = ownershipGuard;
	}

	public Training execute(UUID trainingId, UUID userId, TrainingStatus targetStatus) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("Training not found: " + trainingId));

		// T-058 (anti-IDOR): o treino precisa pertencer a um atleta da carteira
		// do coach autenticado — treino de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(training.getAthleteId(), userId);

		TrainingStatus previousStatus = training.getStatus();
		training.transitionTo(targetStatus);
		Training saved = this.trainingRepository.save(training);

		this.eventPublisher.publishEvent(new TrainingStatusChangedEvent(saved.getId(), saved.getAthleteId(),
				saved.getCoachId(), previousStatus, saved.getStatus()));

		return saved;
	}

}
