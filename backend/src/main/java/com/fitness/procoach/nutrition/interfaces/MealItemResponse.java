package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.FoodKind;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * DTO de saída de um item de refeição (T-085).
 */
@Schema(description = "Item de uma refeição na dieta (T-085). Macros são snapshot no momento da prescrição.")
public record MealItemResponse(
        @Schema(description = "Identificador do item", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID id,
        @Schema(description = "Referência ao catálogo de alimentos (TACO/USDA)", example = "taco:ovo-de-galinha-inteiro") String foodId,
        @Schema(description = "Nome do alimento", example = "Ovo, de galinha, inteiro") String name,
        @Schema(description = "Origem do alimento", example = "TACO") FoodSource source,
        @Schema(description = "Tipo do item", example = "FOOD") FoodKind kind,
        @Schema(description = "Estado do alimento (peso cru ou como servido)", example = "RAW") FoodState state,
        @Schema(description = "Quantidade em gramas (peso cru / como comprado)", example = "100.0") double quantityG,
        @Schema(description = "Medida caseira (ex.: 2 unidades)", example = "2 unidades") String householdMeasure,
        @Schema(description = "Modo de preparo (informativo)", example = "cozido") String preparation,
        @Schema(description = "Calorias (snapshot)", example = "143.0") double calories,
        @Schema(description = "Proteína em gramas (snapshot)", example = "13.0") double protein,
        @Schema(description = "Carboidratos em gramas (snapshot)", example = "1.6") double carbohydrates,
        @Schema(description = "Gorduras em gramas (snapshot)", example = "9.5") double fat) {
}