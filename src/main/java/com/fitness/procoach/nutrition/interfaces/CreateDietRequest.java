package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.DietObjective;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * DTO de entrada da criação da primeira versão (v1) de uma dieta (US-007).
 * Objetivo e macronutrientes são obrigatórios e positivos.
 */
@Schema(description = "Dados para criação da primeira versão (v1) de uma dieta. Regra inegociável (US-015): "
		+ "o recurso criado NUNCA será sobrescrito — alterações posteriores geram uma nova versão encadeada "
		+ "(fluxo POST /api/v1/diets/{dietId}/versions).")
public record CreateDietRequest(
		@Schema(description = "Identificador do atleta",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f")
		@NotNull(message = "athleteId é obrigatório") UUID athleteId,
		@Schema(description = "Objetivo da dieta", example = "HYPERTROPHY")
		@NotNull(message = "objective é obrigatório") DietObjective objective,
		@Schema(description = "Calorias diárias totais (kcal)", example = "3200.0")
		@NotNull(message = "calories é obrigatório") @Positive(message = "calories deve ser positivo") Double calories,
		@Schema(description = "Proteína diária em gramas", example = "180.0")
		@NotNull(message = "protein é obrigatório") @Positive(message = "protein deve ser positivo") Double protein,
		@Schema(description = "Carboidratos diários em gramas", example = "350.0")
		@NotNull(message = "carbohydrates é obrigatório") @Positive(message = "carbohydrates deve ser positivo") Double carbohydrates,
		@Schema(description = "Gorduras diárias em gramas", example = "90.0")
		@NotNull(message = "fat é obrigatório") @Positive(message = "fat deve ser positivo") Double fat,
		@Schema(description = "Identificador de quem criou a dieta (auditoria)",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d")
		@NotNull(message = "createdBy é obrigatório") UUID createdBy) {
}
