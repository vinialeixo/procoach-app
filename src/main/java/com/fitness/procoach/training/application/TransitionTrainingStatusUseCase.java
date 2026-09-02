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

	public TransitionTrainingStatusUseCase(TrainingRepository trainingRepository,
			ApplicationEventPublisher eventPublisher) {
		this.trainingRepository = trainingRepository;
		this.eventPublisher = eventPublisher;
	}

	public Training execute(UUID trainingId, TrainingStatus targetStatus) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new IllegalArgumentException("Training not found: " + trainingId));

		TrainingStatus previousStatus = training.getStatus();
		training.transitionTo(targetStatus);
		Training saved = this.trainingRepository.save(training);

		this.eventPublisher.publishEvent(new TrainingStatusChangedEvent(saved.getId(), saved.getAthleteId(),
				saved.getCoachId(), previousStatus, saved.getStatus()));

		return saved;
	}

}
