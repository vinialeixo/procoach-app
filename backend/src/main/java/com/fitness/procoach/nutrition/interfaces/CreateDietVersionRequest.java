package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.DietObjective;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada da criação de uma nova versão de dieta (US-015/US-016). O
 * motivo da alteração é obrigatório.
 */
@Schema(description = "Dados da nova versão da dieta. O motivo da alteração é obrigatório (US-016).")
public record CreateDietVersionRequest(
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
		@Schema(description = "Motivo da alteração", example = "Ajuste de calorias após reavaliação")
		@NotBlank(message = "changeReason é obrigatório") @Size(max = 255, message = "changeReason deve ter no máximo 255 caracteres") String changeReason) {
}
