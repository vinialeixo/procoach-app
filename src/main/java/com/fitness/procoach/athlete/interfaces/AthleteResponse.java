package com.fitness.procoach.athlete.interfaces;

import com.fitness.procoach.athlete.domain.Athlete;
import com.fitness.procoach.athlete.domain.AthleteStatus;
import com.fitness.procoach.athlete.domain.Goal;
import com.fitness.procoach.athlete.domain.Sex;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de saída de um atleta. O {@code id} único e imutável (US-002) é exposto
 * na ficha do atleta.
 */
public record AthleteResponse(
		UUID id,
		UUID coachId,
		String name,
		Sex sex,
		LocalDate birthDate,
		BigDecimal height,
		BigDecimal weight,
		Goal goal,
		AthleteStatus status,
		Instant createdAt,
		Instant updatedAt) {

	public static AthleteResponse from(Athlete athlete) {
		return new AthleteResponse(athlete.id().value(), athlete.coachId(), athlete.name().value(),
				athlete.sex(), athlete.birthDate(), athlete.height().value(), athlete.weight().value(),
				athlete.goal(), athlete.status(), athlete.createdAt(), athlete.updatedAt());
	}
}
