package com.fitness.procoach.training.interfaces.rest;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criação de uma periodização (macrociclo) do atleta.")
public record CreatePeriodizationRequest(
		@Schema(description = "Identificador do atleta",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f")
		@NotNull(message = "athleteId é obrigatório") UUID athleteId,
		@Schema(description = "Nome da periodização", example = "Macrociclo 2026 - Hipertrofia")
		@NotBlank(message = "name é obrigatório") String name,
		@Schema(description = "Descrição da periodização",
				example = "12 semanas: 8 de hipertrofia + 4 de força, com deload a cada 4 semanas.")
		String description,
		@Schema(description = "Data de início", example = "2026-09-07")
		LocalDate startDate,
		@Schema(description = "Data de término", example = "2026-11-29")
		LocalDate endDate) {
}
