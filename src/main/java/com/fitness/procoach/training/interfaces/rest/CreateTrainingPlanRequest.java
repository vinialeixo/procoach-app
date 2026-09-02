package com.fitness.procoach.training.interfaces.rest;

import java.util.UUID;

public record CreateTrainingPlanRequest(UUID athleteId, UUID coachId, UUID periodizationId, String name,
		String description) {
}
