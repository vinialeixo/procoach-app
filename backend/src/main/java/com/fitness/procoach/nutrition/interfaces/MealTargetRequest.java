package com.fitness.procoach.nutrition.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Metas de macros por refeição (T-085) - request DTO.
 */
@Schema(description = "Metas de macronutrientes por refeição (opcional).")
public record MealTargetRequest(
        @Schema(description = "Calorias alvo", example = "800.0")
        @NotNull(message = "calories é obrigatório") @PositiveOrZero(message = "calories deve ser >= 0") Double calories,
        @Schema(description = "Proteína alvo (g)", example = "50.0")
        @NotNull(message = "protein é obrigatório") @PositiveOrZero(message = "protein deve ser >= 0") Double protein,
        @Schema(description = "Carboidratos alvo (g)", example = "80.0")
        @NotNull(message = "carbohydrates é obrigatório") @PositiveOrZero(message = "carbohydrates deve ser >= 0") Double carbohydrates,
        @Schema(description = "Gorduras alvo (g)", example = "30.0")
        @NotNull(message = "fat é obrigatório") @PositiveOrZero(message = "fat deve ser >= 0") Double fat) {
}