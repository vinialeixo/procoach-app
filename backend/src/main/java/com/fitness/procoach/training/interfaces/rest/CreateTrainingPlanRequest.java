package com.fitness.procoach.training.interfaces.rest;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criação da primeira versão (v1) de um plano de treino. O plano nunca é "
		+ "sobrescrito — alterações posteriores geram uma nova versão (POST /api/training/plans/{id}/versions).")
public record CreateTrainingPlanRequest(
		@Schema(description = "Identificador do atleta",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f")
		@NotNull(message = "athleteId é obrigatório") UUID athleteId,
		@Schema(description = "Identificador da periodização associada",
				example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
		@NotNull(message = "periodizationId é obrigatório") UUID periodizationId,
		@Schema(description = "Nome do plano de treino", example = "Plano de Hipertrofia - Bloco 1")
		@NotBlank(message = "name é obrigatório") String name,
		@Schema(description = "Descrição do plano", example = "Foco em hipertrofia de membros superiores, 4x/semana.")
		String description) {
}
