package com.fitness.procoach.training.interfaces.rest;

import java.time.Instant;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.model.TrainingStatus;

public record TrainingPlanResponse(UUID id, UUID athleteId, UUID coachId, UUID periodizationId, String name,
		String description, int version, UUID previousVersionId, TrainingStatus status, Instant createdAt,
		Instant updatedAt, boolean deleted) {

	public static TrainingPlanResponse from(TrainingPlan tp) {
		return new TrainingPlanResponse(tp.getId(), tp.getAthleteId(), tp.getCoachId(), tp.getPeriodizationId(),
				tp.getName(), tp.getDescription(), tp.getVersion(), tp.getPreviousVersionId(), tp.getStatus(),
				tp.getCreatedAt(), tp.getUpdatedAt(), tp.isDeleted());
	}

}
