package com.fitness.procoach.training.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TrainingCreatedEvent(UUID trainingId, UUID periodizationId, UUID athleteId, UUID coachId,
		String trainingName, Instant occurredAt) {

	public TrainingCreatedEvent(UUID trainingId, UUID periodizationId, UUID athleteId, UUID coachId,
			String trainingName) {
		this(trainingId, periodizationId, athleteId, coachId, trainingName, Instant.now());
	}

}
