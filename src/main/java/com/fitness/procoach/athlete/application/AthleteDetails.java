package com.fitness.procoach.athlete.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fitness.procoach.athlete.domain.Athlete;

/**
 * Visão plana do agregado Athlete para consumo por outros módulos (API pública
 * do módulo athlete). Usa apenas tipos simples para não vazar tipos de domínio
 * pelos limites do Modulith.
 */
public record AthleteDetails(UUID id, UUID coachId, String name, String sex, LocalDate birthDate,
		BigDecimal height, BigDecimal weight, String goal, String status, Instant createdAt, Instant updatedAt) {

	public static AthleteDetails from(Athlete athlete) {
		return new AthleteDetails(athlete.id().value(), athlete.coachId(), athlete.name().value(),
				athlete.sex().name(), athlete.birthDate(), athlete.height().value(), athlete.weight().value(),
				athlete.goal().name(), athlete.status().name(), athlete.createdAt(), athlete.updatedAt());
	}
}
