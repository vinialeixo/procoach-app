package com.fitness.procoach.training.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingStatus;

public record TrainingStatusChangedEvent(UUID trainingId, UUID trainingPlanId, UUID athleteId, UUID coachId,
		TrainingStatus previousStatus, TrainingStatus newStatus, Instant occurredAt) {

	public TrainingStatusChangedEvent(UUID trainingId, UUID athleteId, UUID coachId, TrainingStatus previousStatus,
			TrainingStatus newStatus) {
		this(trainingId, null, athleteId, coachId, previousStatus, newStatus, Instant.now());
	}

	public static TrainingStatusChangedEvent forTrainingPlan(UUID trainingPlanId, UUID athleteId, UUID coachId,
			TrainingStatus previousStatus, TrainingStatus newStatus) {
		return new TrainingStatusChangedEvent(null, trainingPlanId, athleteId, coachId, previousStatus, newStatus,
				Instant.now());
	}

}
