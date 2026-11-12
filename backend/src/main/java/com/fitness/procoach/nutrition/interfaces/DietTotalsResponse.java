package com.fitness.procoach.nutrition.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Totais calculados da dieta a partir das refeições (T-085). Somente leitura.
 */
@Schema(description = "Totais de macronutrientes da dieta calculados a partir das refeições (somente leitura).")
public record DietTotalsResponse(
        @Schema(description = "Calorias totais", example = "3200.0") double calories,
        @Schema(description = "Proteína total (g)", example = "180.0") double protein,
        @Schema(description = "Carboidratos totais (g)", example = "350.0") double carbohydrates,
        @Schema(description = "Gorduras totais (g)", example = "90.0") double fat) {
}