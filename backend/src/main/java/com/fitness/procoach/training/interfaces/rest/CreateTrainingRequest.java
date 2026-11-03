package com.fitness.procoach.training.interfaces.rest;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criação de um treino (sessão) dentro de uma periodização.")
public record CreateTrainingRequest(
		@Schema(description = "Identificador da periodização à qual o treino pertence",
				example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
		@NotNull(message = "periodizationId é obrigatório") UUID periodizationId,
		@Schema(description = "Versão do plano de treino a que a sessão pertence (T-067); precisa ser do mesmo "
				+ "atleta e da mesma periodização", example = "4b3a2c1d-6e5f-4a7b-9c8d-1e2f3a4b5c6d")
		@NotNull(message = "planId é obrigatório") UUID planId,
		@Schema(description = "Identificador do atleta",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f")
		@NotNull(message = "athleteId é obrigatório") UUID athleteId,
		@Schema(description = "Nome do treino", example = "Treino A - Peito e Tríceps")
		@NotBlank(message = "name é obrigatório") String name,
		@Schema(description = "Descrição do treino", example = "Ênfase em força, foco em membros superiores.")
		String description,
		@Schema(description = "Semana da periodização em que o treino ocorre", example = "1")
		int weekNumber,
		@Schema(description = "Dia da semana do treino (1 = segunda-feira, 7 = domingo)", example = "1")
		int dayOfWeek) {
}
