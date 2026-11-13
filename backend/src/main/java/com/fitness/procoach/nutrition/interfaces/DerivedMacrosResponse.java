package com.fitness.procoach.nutrition.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Macronutrientes derivados para resposta da API (T-087).
 * Inclui: g/kg, %kcal, kcal/kg, consistência 4/4/9.
 */
@Schema(description = "Valores derivados dos macronutrientes para exibição e análise.")
public record DerivedMacrosResponse(
        @Schema(description = "Proteína em g/kg de peso corporal", example = "2.2") double proteinGPerKg,
        @Schema(description = "Carboidratos em g/kg de peso corporal", example = "4.5") double carbohydratesGPerKg,
        @Schema(description = "Gorduras em g/kg de peso corporal", example = "1.0") double fatGPerKg,
        @Schema(description = "Proteína em % das calorias totais", example = "22.5") double proteinPctKcal,
        @Schema(description = "Carboidratos em % das calorias totais", example = "50.0") double carbohydratesPctKcal,
        @Schema(description = "Gorduras em % das calorias totais", example = "27.5") double fatPctKcal,
        @Schema(description = "Calorias totais por kg de peso corporal", example = "40.0") double kcalPerKg,
        @Schema(description = "Calorias calculadas por 4/4/9 (proteína*4 + carb*4 + gordura*9)", example = "3200.0") double calculatedCalories,
        @Schema(description = "Calorias declaradas na dieta", example = "3200.0") double declaredCalories,
        @Schema(description = "Delta entre declaradas e calculadas (kcal)", example = "0.0") double consistencyDeltaKcal,
        @Schema(description = "Delta percentual entre declaradas e calculadas (%)", example = "0.0") double consistencyDeltaPercent,
        @Schema(description = "True se calorias declaradas batem com 4/4/9 dentro da tolerância (±5% ou ±50 kcal)", example = "true") boolean isConsistent) {
}