package com.fitness.procoach.nutrition.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * DTO de saída de uma refeição (T-085).
 */
@Schema(description = "Refeição dentro de uma dieta (T-085). Totais derivados dos itens.")
public record MealResponse(
        @Schema(description = "Identificador da refeição", example = "b2c3d4e5-f6a7-8901-bcde-f23456789012") UUID id,
        @Schema(description = "Nome da refeição", example = "Café da manhã") String name,
        @Schema(description = "Ordem da refeição no dia", example = "1") int order,
        @Schema(description = "Horário sugerido (HH:mm)", example = "07:00") String time,
        @Schema(description = "Observações", example = "Pós-treino") String notes,
        @Schema(description = "Metas de macros por refeição (opcional)") MealTargetResponse target,
        @Schema(description = "Itens da refeição") List<MealItemResponse> items,
        @Schema(description = "Totais calculados a partir dos itens (somente leitura)") MealTotalsResponse totals) {
}