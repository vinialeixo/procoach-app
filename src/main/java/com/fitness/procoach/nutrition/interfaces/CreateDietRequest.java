package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.DietObjective;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * DTO de entrada da criação da primeira versão (v1) de uma dieta (US-007).
 * Objetivo e macronutrientes são obrigatórios e positivos.
 */
public record CreateDietRequest(
		@NotNull(message = "coachId é obrigatório") UUID coachId,
		@NotNull(message = "athleteId é obrigatório") UUID athleteId,
		@NotNull(message = "objective é obrigatório") DietObjective objective,
		@NotNull(message = "calories é obrigatório") @Positive(message = "calories deve ser positivo") Double calories,
		@NotNull(message = "protein é obrigatório") @Positive(message = "protein deve ser positivo") Double protein,
		@NotNull(message = "carbohydrates é obrigatório") @Positive(message = "carbohydrates deve ser positivo") Double carbohydrates,
		@NotNull(message = "fat é obrigatório") @Positive(message = "fat deve ser positivo") Double fat,
		@NotNull(message = "createdBy é obrigatório") UUID createdBy) {
}
