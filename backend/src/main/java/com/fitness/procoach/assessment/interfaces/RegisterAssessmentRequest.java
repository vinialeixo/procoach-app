package com.fitness.procoach.assessment.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de entrada do registro de avaliaÃ§Ã£o fÃ­sica (US-004). Data, peso e altura
 * sÃ£o obrigatÃ³rios; as demais medidas sÃ£o opcionais.
 */
@Schema(description = "Dados para registro de uma nova avaliaÃ§Ã£o fÃ­sica. Cada registro Ã© imutÃ¡vel â€” avaliaÃ§Ãµes "
		+ "anteriores nunca sÃ£o sobrescritas.")
public record RegisterAssessmentRequest(
		@Schema(description = "Identificador do atleta avaliado",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f")
		@NotNull(message = "athleteId Ã© obrigatÃ³rio") UUID athleteId,
		@Schema(description = "Data em que a avaliaÃ§Ã£o foi realizada", example = "2026-09-01")
		@NotNull(message = "assessmentDate Ã© obrigatÃ³rio") LocalDate assessmentDate,
		@Schema(description = "Peso em quilogramas", example = "82.4")
		@NotNull(message = "weightKg Ã© obrigatÃ³rio") @Positive(message = "weightKg deve ser positivo") BigDecimal weightKg,
		@Schema(description = "Altura em centÃ­metros", example = "178.0")
		@NotNull(message = "heightCm Ã© obrigatÃ³rio") @Positive(message = "heightCm deve ser positivo") BigDecimal heightCm,
		@Schema(description = "Percentual de gordura corporal (%)", example = "12.5")
		@Positive(message = "bodyFatPercentage deve ser positivo") BigDecimal bodyFatPercentage,
		@Schema(description = "Massa muscular em quilogramas", example = "38.2")
		@Positive(message = "muscleMassKg deve ser positivo") BigDecimal muscleMassKg,
		@Schema(description = "CircunferÃªncia da cintura em centÃ­metros", example = "84.0")
		@Positive(message = "waistCm deve ser positivo") BigDecimal waistCm,
		@Schema(description = "CircunferÃªncia do quadril em centÃ­metros", example = "98.0")
		@Positive(message = "hipCm deve ser positivo") BigDecimal hipCm) {
}
