package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.Food;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de saída de um alimento do catálogo (T-086).
 */
@Schema(description = "Alimento do catálogo TACO com macros por 100 g, porção comestível, medida "
		+ "caseira e estado padrão (RAW/AS_SERVED). Alimentos CUSTOM pertencem ao coach que os criou.")
public record FoodResponse(
		@Schema(description = "Identificador do alimento", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
		UUID id,
		@Schema(description = "Identificador na tabela TACO (null para CUSTOM)",
				example = "taco:frango-peito-sem-pele-cru") String tacoId,
		@Schema(description = "Nome do alimento na nomenclatura TACO",
				example = "Peito de frango, sem pele, cru") String name,
		@Schema(description = "Categoria do alimento", example = "Carnes e derivados") String category,
		@Schema(description = "Macronutrientes por 100 g") MacrosPer100gResponse macrosPer100g,
		@Schema(description = "Porção comestível em percentual do peso", example = "100.0")
		double ediblePortionPercent,
		@Schema(description = "Medida caseira de referência", example = "1 filé médio") String householdMeasure,
		@Schema(description = "Gramas equivalentes à medida caseira", example = "100.0")
		Double householdMeasureGrams,
		@Schema(description = "Origem do alimento (TACO, USDA ou CUSTOM)", example = "TACO") FoodSource source,
		@Schema(description = "Estado padrão para gramatura (RAW ou AS_SERVED)", example = "RAW")
		FoodState stateDefault,
		@Schema(description = "Coach dono do alimento CUSTOM (null para catálogo global)")
		UUID coachId,
		@Schema(description = "true quando o coach aprovou o alimento CUSTOM", example = "false")
		boolean coachApproved,
		@Schema(description = "Data/hora de criação", example = "2026-11-12T10:15:30Z") Instant createdAt,
		@Schema(description = "Data/hora da última atualização", example = "2026-11-12T10:15:30Z")
		Instant updatedAt) {

	public static FoodResponse from(Food food) {
		return new FoodResponse(
				food.id().value(),
				food.tacoId(),
				food.name(),
				food.category(),
				new MacrosPer100gResponse(food.macrosPer100g().calories(), food.macrosPer100g().protein(),
						food.macrosPer100g().carbohydrates(), food.macrosPer100g().fat()),
				food.ediblePortionPercent(),
				food.householdMeasure(),
				food.householdMeasureGrams(),
				food.source(),
				food.stateDefault(),
				food.coachId(),
				food.coachApproved(),
				food.createdAt(),
				food.updatedAt());
	}

	@Schema(description = "Macronutrientes por 100 g do alimento")
	public record MacrosPer100gResponse(
			@Schema(description = "Calorias por 100 g (kcal)", example = "119.0") double calories,
			@Schema(description = "Proteína por 100 g (g)", example = "21.5") double protein,
			@Schema(description = "Carboidratos por 100 g (g)", example = "0.0") double carbohydrates,
			@Schema(description = "Gorduras por 100 g (g)", example = "3.0") double fat) {
	}
}
