package com.fitness.procoach.training.interfaces.rest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.model.PeriodizationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Periodização (macrociclo) de um atleta, com período de início e fim.")
public record PeriodizationResponse(
		@Schema(description = "Identificador da periodização",
				example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d") UUID id,
		@Schema(description = "Identificador do atleta",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f") UUID athleteId,
		@Schema(description = "Identificador do coach responsável",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID coachId,
		@Schema(description = "Nome da periodização", example = "Macrociclo 2026 - Hipertrofia") String name,
		@Schema(description = "Descrição da periodização",
				example = "12 semanas: 8 de hipertrofia + 4 de força, com deload a cada 4 semanas.") String description,
		@Schema(description = "Data de início", example = "2026-09-07") LocalDate startDate,
		@Schema(description = "Data de término", example = "2026-11-29") LocalDate endDate,
		@Schema(description = "Status da periodização (DRAFT, ACTIVE, FINISHED, ARCHIVED)",
				example = "DRAFT") PeriodizationStatus status,
		@Schema(description = "Data/hora de criação do registro", example = "2026-09-01T10:15:30Z") Instant createdAt,
		@Schema(description = "Data/hora da última atualização", example = "2026-09-01T10:15:30Z") Instant updatedAt,
		@Schema(description = "true quando o registro foi excluído logicamente (soft-delete)",
				example = "false") boolean deleted) {

	public static PeriodizationResponse from(Periodization p) {
		return new PeriodizationResponse(p.getId(), p.getAthleteId(), p.getCoachId(), p.getName(), p.getDescription(),
				p.getStartDate(), p.getEndDate(), p.getStatus(), p.getCreatedAt(), p.getUpdatedAt(), p.isDeleted());
	}

}
