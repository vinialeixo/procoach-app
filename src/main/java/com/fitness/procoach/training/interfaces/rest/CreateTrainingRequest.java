package com.fitness.procoach.training.interfaces.rest;

import java.util.UUID;

public record CreateTrainingRequest(UUID periodizationId, UUID athleteId, UUID coachId, String name,
		String description, int weekNumber, int dayOfWeek) {
}
