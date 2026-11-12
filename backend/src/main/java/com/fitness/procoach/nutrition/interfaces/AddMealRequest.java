package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.FoodKind;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO de entrada para adicionar uma refeição à dieta (T-085).
 */
@Schema(description = "Dados para adicionar uma refeição à dieta.")
public record AddMealRequest(
        @Schema(description = "Nome da refeição", example = "Café da manhã")
        @NotBlank(message = "name é obrigatório") @Size(max = 100, message = "name deve ter no máximo 100 caracteres") String name,
        @Schema(description = "Ordem da refeição no dia (0, 1, 2...)", example = "1")
        @NotNull(message = "order é obrigatório") int order,
        @Schema(description = "Horário sugerido (HH:mm)", example = "07:00") String time,
        @Schema(description = "Observações", example = "Pós-treino") String notes,
        @Schema(description = "Metas de macros por refeição (opcional)") MealTargetRequest target) {
}