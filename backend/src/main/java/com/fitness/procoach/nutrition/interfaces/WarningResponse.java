package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.DietWarning;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Warning nutricional estruturado para resposta da API (T-087).
 */
@Schema(description = "Aviso nutricional informativo — nunca bloqueia a dieta; a decisão final é do coach.")
public record WarningResponse(
        @Schema(description = "Código do warning", example = "ZERO_CARBOHYDRATE") String code,
        @Schema(description = "Severidade: INFO, WARNING, CRITICAL", example = "WARNING") String severity,
        @Schema(description = "Mensagem legível para o coach",
                example = "Carboidrato zerado por estratégia (ex.: cetogênica, peak week); monitorar desempenho e hidratação.") String message) {

    public static WarningResponse from(DietWarning warning) {
        return new WarningResponse(warning.code().name(), warning.severity().name(), warning.message());
    }
}