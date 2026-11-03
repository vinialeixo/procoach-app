package com.fitness.procoach.assessment.interfaces;

import com.fitness.procoach.assessment.application.AssessmentApplicationService;
import com.fitness.procoach.assessment.domain.Assessment;
import com.fitness.procoach.assessment.domain.AssessmentMetrics;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de saída de uma avaliação física.
 */
@Schema(description = "Registro imutável de uma avaliação física. Avaliações anteriores nunca são sobrescritas "
		+ "(histórico cronológico preservado).")
public record AssessmentResponse(
		@Schema(description = "Identificador da avaliação",
				example = "5d4c3b2a-1f0e-4d5c-8b7a-6f5e4d3c2b1a") UUID id,
		@Schema(description = "Identificador do atleta avaliado",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f") UUID athleteId,
		@Schema(description = "Identificador do coach responsável",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID coachId,
		@Schema(description = "Data em que a avaliação foi realizada", example = "2026-09-01") LocalDate assessmentDate,
		@Schema(description = "Peso em quilogramas", example = "82.4") BigDecimal weightKg,
		@Schema(description = "Altura em centímetros", example = "178.0") BigDecimal heightCm,
		@Schema(description = "Percentual de gordura corporal (%)", example = "12.5") BigDecimal bodyFatPercentage,
		@Schema(description = "Massa muscular em quilogramas", example = "38.2") BigDecimal muscleMassKg,
		@Schema(description = "Circunferência da cintura em centímetros", example = "84.0") BigDecimal waistCm,
		@Schema(description = "Circunferência do quadril em centímetros", example = "98.0") BigDecimal hipCm,
		@Schema(description = "Data/hora de criação do registro", example = "2026-09-01T10:15:30Z") Instant createdAt,
		@Schema(description = "Métricas derivadas (IMC, TMB, GET e evolução de peso), calculadas no núcleo (T-025)")
		Metrics metrics) {

	/** Métricas derivadas da avaliação; weightChange* é null na primeira avaliação do atleta. */
	public record Metrics(
			@Schema(description = "Índice de massa corporal (kg/m²)", example = "24.69") BigDecimal imc,
			@Schema(description = "Taxa metabólica basal, Mifflin-St Jeor (kcal/dia)", example = "1780") BigDecimal tmbKcal,
			@Schema(description = "Gasto energético total = TMB × fator de atividade (kcal/dia)",
					example = "2136") BigDecimal getKcal,
			@Schema(description = "Variação de peso vs. avaliação anterior (kg)", example = "-1.50") BigDecimal weightChangeKg,
			@Schema(description = "Variação de peso vs. avaliação anterior (%)", example = "-1.84") BigDecimal weightChangePercent) {

		static Metrics from(AssessmentMetrics metrics) {
			return new Metrics(metrics.imc(), metrics.tmbKcal(), metrics.getKcal(), metrics.weightChangeKg(),
					metrics.weightChangePercent());
		}
	}

	public static AssessmentResponse from(AssessmentApplicationService.AssessmentView view) {
		Assessment assessment = view.assessment();
		return new AssessmentResponse(assessment.id().value(), assessment.athleteId(),
				assessment.coachId(), assessment.assessmentDate(),
				assessment.measurements().weightKg(), assessment.measurements().heightCm(),
				assessment.measurements().bodyFatPercentage(), assessment.measurements().muscleMassKg(),
				assessment.measurements().waistCm(), assessment.measurements().hipCm(),
				assessment.createdAt(), Metrics.from(view.metrics()));
	}
}
