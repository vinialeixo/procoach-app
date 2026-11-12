package com.fitness.procoach.nutrition.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metas de macros por refeição (T-085).
 */
@Schema(description = "Metas de macronutrientes por refeição (opcional).")
public record MealTargetResponse(
        @Schema(description = "Calorias alvo", example = "800.0") double calories,
        @Schema(description = "Proteína alvo (g)", example = "50.0") double protein,
        @Schema(description = "Carboidratos alvo (g)", example = "80.0") double carbohydrates,
        @Schema(description = "Gorduras alvo (g)", example = "30.0") double fat) {
}