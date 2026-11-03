package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de saída de uma dieta.
 */
@Schema(description = "Versão de uma dieta. Regra inegociável (US-015): uma dieta NUNCA é sobrescrita — toda "
		+ "alteração gera uma nova versão encadeada (v1→v2→v3) e as versões anteriores tornam-se somente "
		+ "leitura (readOnly=true).")
public record DietResponse(
		@Schema(description = "Identificador desta versão da dieta",
				example = "5d4c3b2a-1f0e-4d5c-8b7a-6f5e4d3c2b1a") UUID id,
		@Schema(description = "Identificador do coach responsável",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID coachId,
		@Schema(description = "Identificador do atleta",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f") UUID athleteId,
		@Schema(description = "Objetivo da dieta", example = "HYPERTROPHY") DietObjective objective,
		@Schema(description = "Calorias diárias totais (kcal)", example = "3200.0") double calories,
		@Schema(description = "Proteína diária em gramas", example = "180.0") double protein,
		@Schema(description = "Carboidratos diários em gramas", example = "350.0") double carbohydrates,
		@Schema(description = "Gorduras diárias em gramas", example = "90.0") double fat,
		@Schema(description = "Estado da dieta (Rascunho, Em Revisão, Aprovada, Arquivada)",
				example = "DRAFT") DietStatus status,
		@Schema(description = "Número desta versão na cadeia de versionamento (v1, v2, ...)", example = "1") int version,
		@Schema(description = "Identificador da versão anterior na cadeia (null na v1) — evidência de que a "
				+ "dieta nunca é sobrescrita (US-015)",
				example = "5d4c3b2a-1f0e-4d5c-8b7a-6f5e4d3c2b1a") UUID previousVersionId,
		@Schema(description = "Motivo da alteração registrado na criação desta versão",
				example = "Ajuste de calorias após reavaliação") String changeReason,
		@Schema(description = "Data/hora de criação do registro", example = "2026-09-01T10:15:30Z") Instant createdAt,
		@Schema(description = "Data/hora da última atualização", example = "2026-09-01T10:15:30Z") Instant updatedAt,
		@Schema(description = "true quando esta versão foi substituída por uma mais recente e tornou-se somente "
				+ "leitura — nunca pode ser alterada ou sobrescrita (US-015)", example = "false") boolean readOnly,
		@Schema(description = "true quando o registro foi excluído logicamente (soft-delete)",
				example = "false") boolean deleted,
		@Schema(description = "Usuário que criou esta versão (auditoria, US-016)",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID createdBy,
		@Schema(description = "Último usuário que alterou o registro",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID updatedBy) {

	public static DietResponse from(Diet diet) {
		return new DietResponse(diet.id().value(), diet.coachId().value(), diet.athleteId().value(),
				diet.objective(), diet.macros().calories(), diet.macros().protein(),
				diet.macros().carbohydrates(), diet.macros().fat(), diet.status(), diet.version(),
				diet.previousVersionId() == null ? null : diet.previousVersionId().value(),
				diet.changeReason(), diet.createdAt(), diet.updatedAt(), diet.isReadOnly(),
				diet.isDeleted(), diet.createdBy(), diet.updatedBy());
	}
}
