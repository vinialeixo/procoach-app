package com.fitness.procoach.training.interfaces.rest;

import java.time.Instant;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.model.TrainingStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Versão de um plano de treino. O plano nunca é sobrescrito: toda alteração gera uma nova "
		+ "versão encadeada (v1→v2→v3) com referência à anterior (previousVersionId).")
public record TrainingPlanResponse(
		@Schema(description = "Identificador desta versão do plano",
				example = "5d4c3b2a-1f0e-4d5c-8b7a-6f5e4d3c2b1a") UUID id,
		@Schema(description = "Identificador do atleta",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f") UUID athleteId,
		@Schema(description = "Identificador do coach responsável",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID coachId,
		@Schema(description = "Identificador da periodização associada",
				example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d") UUID periodizationId,
		@Schema(description = "Nome do plano de treino", example = "Plano de Hipertrofia - Bloco 1") String name,
		@Schema(description = "Descrição do plano",
				example = "Foco em hipertrofia de membros superiores, 4x/semana.") String description,
		@Schema(description = "Número desta versão na cadeia de versionamento (v1, v2, ...)", example = "1") int version,
		@Schema(description = "Identificador da versão anterior na cadeia (null na v1)",
				example = "5d4c3b2a-1f0e-4d5c-8b7a-6f5e4d3c2b1a") UUID previousVersionId,
		@Schema(description = "Status do plano (DRAFT, IN_REVIEW, ACTIVE, FINISHED)",
				example = "DRAFT") TrainingStatus status,
		@Schema(description = "Data/hora de criação do registro", example = "2026-09-01T10:15:30Z") Instant createdAt,
		@Schema(description = "Data/hora da última atualização", example = "2026-09-01T10:15:30Z") Instant updatedAt,
		@Schema(description = "true quando o registro foi excluído logicamente (soft-delete)",
				example = "false") boolean deleted) {

	public static TrainingPlanResponse from(TrainingPlan tp) {
		return new TrainingPlanResponse(tp.getId(), tp.getAthleteId(), tp.getCoachId(), tp.getPeriodizationId(),
				tp.getName(), tp.getDescription(), tp.getVersion(), tp.getPreviousVersionId(), tp.getStatus(),
				tp.getCreatedAt(), tp.getUpdatedAt(), tp.isDeleted());
	}

}
