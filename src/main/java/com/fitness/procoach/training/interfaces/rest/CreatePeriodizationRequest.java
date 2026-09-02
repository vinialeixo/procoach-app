package com.fitness.procoach.training.interfaces.rest;

import java.time.LocalDate;
import java.util.UUID;

public record CreatePeriodizationRequest(UUID athleteId, UUID coachId, String name, String description,
		LocalDate startDate, LocalDate endDate) {
}
