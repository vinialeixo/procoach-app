package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.FoodKind;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO de entrada para adicionar um item a uma refeição (T-085).
 */
@Schema(description = "Dados para adicionar um item de alimento a uma refeição.")
public record AddMealItemRequest(
        @Schema(description = "Referência ao catálogo de alimentos (TACO/USDA)", example = "taco:ovo-de-galinha-inteiro") String foodId,
        @Schema(description = "Nome do alimento", example = "Ovo, de galinha, inteiro")
        @NotBlank(message = "name é obrigatório") @Size(max = 200, message = "name deve ter no máximo 200 caracteres") String name,
        @Schema(description = "Origem do alimento", example = "TACO")
        @NotNull(message = "source é obrigatório") FoodSource source,
        @Schema(description = "Tipo do item (FOOD ou SUPPLEMENT_CALORIC)", example = "FOOD")
        @NotNull(message = "kind é obrigatório") FoodKind kind,
        @Schema(description = "Estado do alimento (RAW ou AS_SERVED)", example = "RAW")
        @NotNull(message = "state é obrigatório") FoodState state,
        @Schema(description = "Quantidade em gramas (peso cru / como comprado)", example = "100.0")
        @NotNull(message = "quantityG é obrigatório") @Positive(message = "quantityG deve ser positivo") Double quantityG,
        @Schema(description = "Medida caseira (ex.: 2 unidades)", example = "2 unidades") String householdMeasure,
        @Schema(description = "Modo de preparo (informativo)", example = "cozido") String preparation,
        @Schema(description = "Calorias (snapshot)", example = "143.0")
        @NotNull(message = "calories é obrigatório") @PositiveOrZero(message = "calories deve ser >= 0") Double calories,
        @Schema(description = "Proteína em gramas (snapshot)", example = "13.0")
        @NotNull(message = "protein é obrigatório") @PositiveOrZero(message = "protein deve ser >= 0") Double protein,
        @Schema(description = "Carboidratos em gramas (snapshot)", example = "1.6")
        @NotNull(message = "carbohydrates é obrigatório") @PositiveOrZero(message = "carbohydrates deve ser >= 0") Double carbohydrates,
        @Schema(description = "Gorduras em gramas (snapshot)", example = "9.5")
        @NotNull(message = "fat é obrigatório") @PositiveOrZero(message = "fat deve ser >= 0") Double fat) {
}