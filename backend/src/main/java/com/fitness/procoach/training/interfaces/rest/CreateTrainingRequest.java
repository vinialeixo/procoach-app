package com.fitness.procoach.training.interfaces.rest;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para criação de um treino (sessão) dentro de uma periodização.")
public record CreateTrainingRequest(
		@Schema(description = "Identificador da periodização à qual o treino pertence",
				example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
		UUID periodizationId,
		@Schema(description = "Identificador do atleta",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f")
		UUID athleteId,
		@Schema(description = "Nome do treino", example = "Treino A - Peito e Tríceps")
		String name,
		@Schema(description = "Descrição do treino", example = "Ênfase em força, foco em membros superiores.")
		String description,
		@Schema(description = "Semana da periodização em que o treino ocorre", example = "1")
		int weekNumber,
		@Schema(description = "Dia da semana do treino (1 = segunda-feira, 7 = domingo)", example = "1")
		int dayOfWeek) {
}
