package com.fitness.procoach.training.interfaces.rest;

import com.fitness.procoach.training.domain.model.IntensityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Novos valores de uma prescrição de exercício já criada. A posição no treino não muda "
		+ "(use /exercises/reorder) e exercício personalizado mantém o próprio id.")
public record UpdateExercisePrescriptionRequest(
		@Schema(description = "Id do exercício da biblioteca; informe o currentExerciseId para trocar pela versão "
				+ "mais recente (null mantém o atual)", example = "9f1e2d3c-4b5a-4c6d-8e7f-0a1b2c3d4e5f")
		UUID exerciseId,
		@Schema(description = "Nome do exercício", example = "Supino reto com barra")
		@NotBlank(message = "exerciseName é obrigatório") String exerciseName,
		@Schema(description = "Número de séries", example = "5") int sets,
		@Schema(description = "Repetições por série", example = "12") int repetitions,
		@Schema(description = "Carga (kg)", example = "62.5")
		@NotNull(message = "load é obrigatório") BigDecimal load,
		@Schema(description = "Tipo de intensidade: PERCENT_1RM, RPE, RIR ou ABSOLUTE_LOAD", example = "RPE")
		@NotNull(message = "intensityType é obrigatório") IntensityType intensityType,
		@Schema(description = "Valor da intensidade", example = "8.5")
		@NotNull(message = "intensityValue é obrigatório") BigDecimal intensityValue,
		@Schema(description = "Descanso entre séries, em segundos", example = "90") int restSeconds,
		@Schema(description = "Observações do coach", example = "Cadência controlada.") String notes) {
}
