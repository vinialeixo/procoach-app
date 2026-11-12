package com.fitness.procoach.nutrition.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Totais calculados de uma refeição (T-085). Somente leitura — derivados dos itens.
 */
@Schema(description = "Totais de macronutrientes da refeição calculados a partir dos itens.")
public record MealTotalsResponse(
        @Schema(description = "Calorias totais", example = "800.0") double calories,
        @Schema(description = "Proteína total (g)", example = "50.0") double protein,
        @Schema(description = "Carboidratos totais (g)", example = "80.0") double carbohydrates,
        @Schema(description = "Gorduras totais (g)", example = "30.0") double fat) {
}