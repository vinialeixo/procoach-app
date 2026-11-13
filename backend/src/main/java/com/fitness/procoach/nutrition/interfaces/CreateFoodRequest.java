package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.FoodState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para criar um alimento CUSTOM (T-086). O coachId nunca vem do
 * corpo: é resolvido a partir do JWT.
 */
@Schema(description = "Dados para criar um alimento CUSTOM. O coachId é resolvido pelo JWT.")
public record CreateFoodRequest(
		@Schema(description = "Nome do alimento", example = "Whey protein isolado, sabor baunilha")
		@NotBlank(message = "name é obrigatório")
		@Size(max = 200, message = "name deve ter no máximo 200 caracteres") String name,
		@Schema(description = "Categoria do alimento", example = "Suplementos")
		@Size(max = 100, message = "category deve ter no máximo 100 caracteres") String category,
		@Schema(description = "Calorias por 100 g (kcal)", example = "380.0")
		@NotNull(message = "calories é obrigatório")
		@PositiveOrZero(message = "calories deve ser >= 0") Double calories,
		@Schema(description = "Proteína por 100 g (g)", example = "80.0")
		@NotNull(message = "protein é obrigatório")
		@PositiveOrZero(message = "protein deve ser >= 0") Double protein,
		@Schema(description = "Carboidratos por 100 g (g)", example = "5.0")
		@NotNull(message = "carbohydrates é obrigatório")
		@PositiveOrZero(message = "carbohydrates deve ser >= 0") Double carbohydrates,
		@Schema(description = "Gorduras por 100 g (g)", example = "4.0")
		@NotNull(message = "fat é obrigatório")
		@PositiveOrZero(message = "fat deve ser >= 0") Double fat,
		@Schema(description = "Porção comestível em percentual (default 100)", example = "100.0")
		@DecimalMin(value = "0.0", inclusive = false,
				message = "ediblePortionPercent deve ser > 0")
		@DecimalMax(value = "100.0", message = "ediblePortionPercent deve ser <= 100")
		Double ediblePortionPercent,
		@Schema(description = "Medida caseira de referência", example = "1 scoop")
		@Size(max = 100, message = "householdMeasure deve ter no máximo 100 caracteres")
		String householdMeasure,
		@Schema(description = "Gramas equivalentes à medida caseira", example = "30.0")
		@Positive(message = "householdMeasureGrams deve ser positivo") Double householdMeasureGrams,
		@Schema(description = "Estado padrão para gramatura (RAW ou AS_SERVED)", example = "AS_SERVED")
		@NotNull(message = "stateDefault é obrigatório") FoodState stateDefault) {
}
