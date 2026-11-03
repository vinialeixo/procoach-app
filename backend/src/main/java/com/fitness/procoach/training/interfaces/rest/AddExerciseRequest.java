package com.fitness.procoach.training.interfaces.rest;

import java.math.BigDecimal;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.IntensityType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Prescrição de um exercício para um treino (séries, repetições, carga, intensidade e descanso).")
public record AddExerciseRequest(
		@Schema(description = "Identificador do exercício da biblioteca (null quando customExercise=true)",
				example = "3a4b5c6d-7e8f-4a9b-0c1d-2e3f4a5b6c7d")
		UUID exerciseId,
		@Schema(description = "Nome do exercício (obrigatório quando customExercise=true)",
				example = "Supino reto com barra")
		String exerciseName,
		@Schema(description = "true quando é um exercício personalizado do coach, fora da biblioteca",
				example = "false")
		boolean customExercise,
		@Schema(description = "Posição do exercício na ordem de execução do treino", example = "1")
		int orderIndex,
		@Schema(description = "Número de séries", example = "4")
		int sets,
		@Schema(description = "Repetições por série", example = "10")
		int repetitions,
		@Schema(description = "Carga utilizada (kg)", example = "60.0")
		BigDecimal load,
		@Schema(description = "Tipo de intensidade: PERCENT_1RM, RPE, RIR ou ABSOLUTE_LOAD", example = "RPE")
		IntensityType intensityType,
		@Schema(description = "Valor da intensidade (conforme o tipo: %1RM, RPE, RIR ou carga absoluta)",
				example = "8.0")
		BigDecimal intensityValue,
		@Schema(description = "Descanso entre séries, em segundos", example = "90")
		int restSeconds,
		@Schema(description = "Observações do coach sobre a prescrição",
				example = "Cadência controlada na fase excêntrica.")
		String notes) {
}
