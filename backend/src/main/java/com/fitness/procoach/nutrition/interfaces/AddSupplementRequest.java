package com.fitness.procoach.nutrition.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para adicionar um suplemento à dieta (T-090).
 */
@Schema(description = "Request para adicionar um suplemento à dieta. Suplementos não calóricos (creatina, "
        + "multivitamínico) têm countsTowardMacros=false. Suplementos calóricos (Whey) têm "
        + "countsTowardMacros=true e entram como MealItem com kind=SUPPLEMENT_CALORIC.")
public record AddSupplementRequest(
        @Schema(description = "Nome do suplemento", example = "Creatina monoidratada", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String name,

        @Schema(description = "Dose", example = "5 g", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String dose,

        @Schema(description = "Horário de consumo", example = "qualquer horário")
        String timing,

        @Schema(description = "Se true, o suplemento conta para os macros (é calórico, como Whey); "
                + "se false, não conta (creatina, multivitamínico, etc.)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Boolean countsTowardMacros) {
}