package com.fitness.procoach.training.interfaces.rest;

import java.math.BigDecimal;
import java.util.UUID;

import com.fitness.procoach.library.application.ExerciseApplicationService;
import com.fitness.procoach.training.domain.model.ExercisePrescription;
import com.fitness.procoach.training.domain.model.IntensityType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Prescrição de um exercício dentro de um treino (séries, repetições, carga, intensidade e descanso).")
public record ExercisePrescriptionResponse(
		@Schema(description = "Identificador da prescrição",
				example = "9f8e7d6c-5b4a-4c3d-2e1f-0a9b8c7d6e5f") UUID id,
		@Schema(description = "Identificador do exercício da biblioteca (null quando customExercise=true)",
				example = "3a4b5c6d-7e8f-4a9b-0c1d-2e3f4a5b6c7d") UUID exerciseId,
		@Schema(description = "Nome do exercício", example = "Supino reto com barra") String exerciseName,
		@Schema(description = "true quando é um exercício personalizado do coach, fora da biblioteca",
				example = "false") boolean customExercise,
		@Schema(description = "Posição do exercício na ordem de execução do treino", example = "1") int orderIndex,
		@Schema(description = "Número de séries", example = "4") int sets,
		@Schema(description = "Repetições por série", example = "10") int repetitions,
		@Schema(description = "Carga utilizada (kg)", example = "60.0") BigDecimal load,
		@Schema(description = "Tipo de intensidade: PERCENT_1RM, RPE, RIR ou ABSOLUTE_LOAD",
				example = "RPE") IntensityType intensityType,
		@Schema(description = "Valor da intensidade (conforme o tipo: %1RM, RPE, RIR ou carga absoluta)",
				example = "8.0") BigDecimal intensityValue,
		@Schema(description = "Descanso entre séries, em segundos", example = "90") int restSeconds,
		@Schema(description = "Observações do coach sobre a prescrição",
				example = "Cadência controlada na fase excêntrica.") String notes,
		@Schema(description = "Versão do exercício da biblioteca que foi prescrita (null para exercício personalizado)",
				example = "1") Integer exerciseVersion,
		@Schema(description = "true quando o exercício da biblioteca já tem uma versão mais nova — esta prescrição "
				+ "continua apontando para a versão que o coach usou", example = "false") boolean newerVersionAvailable,
		@Schema(description = "Id da versão corrente do exercício, quando há versão mais nova (null caso contrário)",
				example = "9f1e2d3c-4b5a-4c6d-8e7f-0a1b2c3d4e5f") UUID currentExerciseId) {

	public static ExercisePrescriptionResponse from(ExercisePrescription e) {
		return from(e, null);
	}

	/** Com o status de versão do exercício da biblioteca (null para exercício personalizado). */
	public static ExercisePrescriptionResponse from(ExercisePrescription e,
			ExerciseApplicationService.ExerciseVersionStatus versionStatus) {
		return new ExercisePrescriptionResponse(e.getId(), e.getExerciseId(), e.getExerciseName(), e.isCustomExercise(),
				e.getOrderIndex(), e.getSets(), e.getRepetitions(), e.getLoad(), e.getIntensityType(),
				e.getIntensityValue(), e.getRestSeconds(), e.getNotes(),
				versionStatus == null ? null : versionStatus.version(),
				versionStatus != null && versionStatus.newerVersionAvailable(),
				versionStatus == null ? null : versionStatus.currentVersionId());
	}

}
