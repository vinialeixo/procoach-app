package com.fitness.procoach.training.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TrainingPlanCreatedEvent(UUID trainingPlanId, UUID periodizationId, UUID athleteId, UUID coachId,
		String planName, int version, UUID previousVersionId, Instant occurredAt) {

	public TrainingPlanCreatedEvent(UUID trainingPlanId, UUID periodizationId, UUID athleteId, UUID coachId,
			String planName, int version, UUID previousVersionId) {
		this(trainingPlanId, periodizationId, athleteId, coachId, planName, version, previousVersionId,
				Instant.now());
	}

}
