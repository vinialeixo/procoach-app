package com.fitness.procoach.library.interfaces;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fitness.procoach.library.domain.Exercise;
import com.fitness.procoach.library.domain.Equipment;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.MuscleGroup;

import io.swagger.v3.oas.annotations.media.Schema;

/** DTO de saída de um exercício. */
@Schema(description = "Exercício da biblioteca, com execução, erros comuns, progressões, regressões, "
		+ "contraindicações e referências científicas.")
public record ExerciseResponse(
		@Schema(description = "Identificador do exercício",
				example = "3a4b5c6d-7e8f-4a9b-0c1d-2e3f4a5b6c7d") UUID id,
		@Schema(description = "Nome do exercício", example = "Supino reto com barra") String name,
		@Schema(description = "Grupos musculares trabalhados",
				example = "[\"PEITO\", \"TRICEPS\"]") List<MuscleGroup> muscleGroups,
		@Schema(description = "Equipamento necessário", example = "BARRA_LIVRE") Equipment equipment,
		@Schema(description = "Descrição da execução correta do movimento",
				example = "Deitado no banco, desça a barra controlada até o peito e empurre de volta.") String execution,
		@Schema(description = "Erros comuns de execução",
				example = "[\"Arquear a lombar\", \"Quicar a barra no peito\"]") List<String> commonErrors,
		@Schema(description = "Progressões (variações mais difíceis)",
				example = "[\"Supino inclinado com barra\"]") List<String> progressions,
		@Schema(description = "Regressões (variações mais fáceis)",
				example = "[\"Supino com halteres\", \"Flexão de braço\"]") List<String> regressions,
		@Schema(description = "Contraindicações",
				example = "[\"Lesão no ombro em fase aguda\"]") List<String> contraindications,
		@Schema(description = "Referências científicas que embasam o exercício") List<ExerciseReference> references,
		@Schema(description = "Identificador de quem criou o exercício",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID createdBy,
		@Schema(description = "Data/hora de criação do registro", example = "2026-09-01T10:15:30Z") Instant createdAt,
		@Schema(description = "Data/hora da última atualização", example = "2026-09-01T10:15:30Z") Instant updatedAt,
		@Schema(description = "Número da versão do exercício (v1, v2, ...) — editar cria uma nova versão", example = "1") int version,
		@Schema(description = "Versão anterior na cadeia (null na v1)",
				example = "3a4b5c6d-7e8f-4a9b-0c1d-2e3f4a5b6c7d") UUID previousVersionId,
		@Schema(description = "true quando uma versão mais nova substituiu esta; a versão antiga continua legível "
				+ "(treinos já prescritos apontam para ela) mas não aparece nas buscas", example = "false") boolean superseded) {

	public static ExerciseResponse from(Exercise e) {
		return new ExerciseResponse(e.getId(), e.getName(), e.getMuscleGroups(), e.getEquipment(),
				e.getExecution(), e.getCommonErrors(), e.getProgressions(), e.getRegressions(),
				e.getContraindications(), e.getReferences(), e.getCreatedBy(), e.getCreatedAt(),
				e.getUpdatedAt(), e.getVersion(), e.getPreviousVersionId(), e.isSuperseded());
	}
}
