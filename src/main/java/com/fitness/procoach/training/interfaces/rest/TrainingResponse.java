package com.fitness.procoach.training.interfaces.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingStatus;

public record TrainingResponse(UUID id, UUID periodizationId, UUID athleteId, UUID coachId, String name,
		String description, int weekNumber, int dayOfWeek, TrainingStatus status,
		List<ExercisePrescriptionResponse> exercises, Instant createdAt, Instant updatedAt, boolean deleted) {

	public static TrainingResponse from(Training t) {
		List<ExercisePrescriptionResponse> exerciseResponses = t.getExercises()
			.stream()
			.map(ExercisePrescriptionResponse::from)
			.toList();
		return new TrainingResponse(t.getId(), t.getPeriodizationId(), t.getAthleteId(), t.getCoachId(), t.getName(),
				t.getDescription(), t.getWeekNumber(), t.getDayOfWeek(), t.getStatus(), exerciseResponses,
				t.getCreatedAt(), t.getUpdatedAt(), t.isDeleted());
	}

}
