package com.fitness.procoach.training.interfaces.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Treino (sessão) de uma periodização, com as prescrições de exercícios em ordem de execução.")
public record TrainingResponse(
		@Schema(description = "Identificador do treino",
				example = "6e5d4c3b-2a1f-4e5d-9c8b-7a6f5e4d3c2b") UUID id,
		@Schema(description = "Identificador da periodização à qual o treino pertence",
				example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d") UUID periodizationId,
		@Schema(description = "Identificador do atleta",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f") UUID athleteId,
		@Schema(description = "Identificador do coach responsável",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID coachId,
		@Schema(description = "Nome do treino", example = "Treino A - Peito e Tríceps") String name,
		@Schema(description = "Descrição do treino", example = "Ênfase em força, foco em membros superiores.") String description,
		@Schema(description = "Semana da periodização em que o treino ocorre", example = "1") int weekNumber,
		@Schema(description = "Dia da semana do treino (1 = segunda-feira, 7 = domingo)", example = "1") int dayOfWeek,
		@Schema(description = "Status do treino (DRAFT, IN_REVIEW, ACTIVE, FINISHED)", example = "DRAFT") TrainingStatus status,
		@Schema(description = "Prescrições de exercícios na ordem de execução") List<ExercisePrescriptionResponse> exercises,
		@Schema(description = "Data/hora de criação do registro", example = "2026-09-01T10:15:30Z") Instant createdAt,
		@Schema(description = "Data/hora da última atualização", example = "2026-09-01T10:15:30Z") Instant updatedAt,
		@Schema(description = "true quando o registro foi excluído logicamente (soft-delete)",
				example = "false") boolean deleted) {

	public static TrainingResponse from(Training t) {
		List<ExercisePrescriptionResponse> exerciseResponses = t.getExercises()
			.stream()
			.map(ExercisePrescriptionResponse::from)
			.toList();
		return new TrainingResponse(t.getId(), t.getPeriodizationId(), t.getAthleteId(), t.getCoachId(), t.getName(),
				t.getDescription(), t.getWeekNumber(), t.getDayOfWeek(), t.getStatus(), exerciseResponses,
				t.getCreatedAt(), t.getUpdatedAt(), t.isDeleted());
	}

}
