package com.fitness.procoach.library.interfaces;

import java.util.List;
import java.util.UUID;

import com.fitness.procoach.library.domain.Equipment;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.MuscleGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** DTO de entrada para criação de exercício. */
@Schema(description = "Dados para criação de um exercício na biblioteca, com embasamento científico.")
public record CreateExerciseRequest(
		@Schema(description = "Nome do exercício", example = "Supino reto com barra")
		@NotBlank String name,
		@Schema(description = "Grupos musculares trabalhados", example = "[\"PEITO\", \"TRICEPS\"]")
		@NotEmpty List<MuscleGroup> muscleGroups,
		@Schema(description = "Equipamento necessário", example = "BARRA_LIVRE")
		@NotNull Equipment equipment,
		@Schema(description = "Descrição da execução correta do movimento",
				example = "Deitado no banco, desça a barra controlada até o peito e empurre de volta.")
		@NotBlank String execution,
		@Schema(description = "Erros comuns de execução",
				example = "[\"Arquear a lombar\", \"Quicar a barra no peito\"]")
		List<String> commonErrors,
		@Schema(description = "Progressões (variações mais difíceis)",
				example = "[\"Supino inclinado com barra\"]")
		List<String> progressions,
		@Schema(description = "Regressões (variações mais fáceis)",
				example = "[\"Supino com halteres\", \"Flexão de braço\"]")
		List<String> regressions,
		@Schema(description = "Contraindicações",
				example = "[\"Lesão no ombro em fase aguda\"]")
		List<String> contraindications,
		@Schema(description = "Referências científicas que embasam o exercício")
		List<ExerciseReference> references,
		@Schema(description = "Identificador de quem criou o exercício (auditoria)",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d")
		@NotNull UUID createdBy) {
}
