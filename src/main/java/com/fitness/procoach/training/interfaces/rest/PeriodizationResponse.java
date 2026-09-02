package com.fitness.procoach.training.interfaces.rest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.model.PeriodizationStatus;

public record PeriodizationResponse(UUID id, UUID athleteId, UUID coachId, String name, String description,
		LocalDate startDate, LocalDate endDate, PeriodizationStatus status, Instant createdAt, Instant updatedAt,
		boolean deleted) {

	public static PeriodizationResponse from(Periodization p) {
		return new PeriodizationResponse(p.getId(), p.getAthleteId(), p.getCoachId(), p.getName(), p.getDescription(),
				p.getStartDate(), p.getEndDate(), p.getStatus(), p.getCreatedAt(), p.getUpdatedAt(), p.isDeleted());
	}

}
