package com.fitness.procoach.assessment.interfaces;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de entrada do registro de avaliação física (US-004). Data, peso e altura
 * são obrigatórios; as demais medidas são opcionais.
 */
public record RegisterAssessmentRequest(
		@NotNull(message = "athleteId é obrigatório") UUID athleteId,
		@NotNull(message = "coachId é obrigatório") UUID coachId,
		@NotNull(message = "assessmentDate é obrigatório") LocalDate assessmentDate,
		@NotNull(message = "weightKg é obrigatório") @Positive(message = "weightKg deve ser positivo") BigDecimal weightKg,
		@NotNull(message = "heightCm é obrigatório") @Positive(message = "heightCm deve ser positivo") BigDecimal heightCm,
		@Positive(message = "bodyFatPercentage deve ser positivo") BigDecimal bodyFatPercentage,
		@Positive(message = "muscleMassKg deve ser positivo") BigDecimal muscleMassKg,
		@Positive(message = "waistCm deve ser positivo") BigDecimal waistCm,
		@Positive(message = "hipCm deve ser positivo") BigDecimal hipCm) {
}
