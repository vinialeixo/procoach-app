package com.fitness.procoach.nutrition.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response de um suplemento na dieta (T-090).
 */
@Schema(description = "Suplemento na dieta. Suplementos não calóricos (creatina, multivitamínico) têm "
        + "countsTowardMacros=false. Suplementos calóricos (Whey) têm countsTowardMacros=true e entram "
        + "como MealItem com kind=SUPPLEMENT_CALORIC.")
public record SupplementResponse(
        @Schema(description = "Nome do suplemento", example = "Creatina monoidratada") String name,
        @Schema(description = "Dose", example = "5 g") String dose,
        @Schema(description = "Horário de consumo", example = "qualquer horário") String timing,
        @Schema(description = "Se true, o suplemento conta para os macros (é calórico, como Whey); "
                + "se false, não conta (creatina, multivitamínico, etc.)", example = "false") boolean countsTowardMacros) {

    public static SupplementResponse from(com.fitness.procoach.nutrition.domain.Supplement supplement) {
        return new SupplementResponse(supplement.name(), supplement.dose(), supplement.timing(), supplement.countsTowardMacros());
    }
}