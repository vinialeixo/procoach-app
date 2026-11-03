package com.fitness.procoach.assessment.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

/**
 * Comparação lado a lado de duas avaliações do mesmo atleta (T-026/US-06). Toda
 * variação é {@code to - from}; o núcleo calcula, o front só exibe. Quando uma das
 * pontas não tem a medida (campos opcionais), a variação fica {@code null}.
 */
public record AssessmentComparison(long daysBetween, Delta weightKg, Delta bodyFatPercentage,
		Delta muscleMassKg, Delta waistCm, Delta hipCm, Delta imc, Delta tmbKcal, Delta getKcal) {

	/**
	 * Valor nas duas avaliações e a diferença entre elas.
	 *
	 * @param change         {@code to - from}, 2 casas
	 * @param changePercent  {@code change / from * 100}, 2 casas; {@code null} se {@code from} é zero
	 */
	public record Delta(BigDecimal from, BigDecimal to, BigDecimal change, BigDecimal changePercent) {

		static Delta of(BigDecimal from, BigDecimal to) {
			if (from == null || to == null) {
				return new Delta(from, to, null, null);
			}
			BigDecimal change = to.subtract(from).setScale(2, RoundingMode.HALF_UP);
			BigDecimal percent = from.signum() == 0 ? null
					: to.subtract(from).multiply(BigDecimal.valueOf(100)).divide(from, 2, RoundingMode.HALF_UP);
			return new Delta(from, to, change, percent);
		}
	}

	public static AssessmentComparison of(Assessment from, AssessmentMetrics fromMetrics, Assessment to,
			AssessmentMetrics toMetrics) {
		BodyMeasurements a = from.measurements();
		BodyMeasurements b = to.measurements();
		return new AssessmentComparison(
				ChronoUnit.DAYS.between(from.assessmentDate(), to.assessmentDate()),
				Delta.of(a.weightKg(), b.weightKg()),
				Delta.of(a.bodyFatPercentage(), b.bodyFatPercentage()),
				Delta.of(a.muscleMassKg(), b.muscleMassKg()),
				Delta.of(a.waistCm(), b.waistCm()),
				Delta.of(a.hipCm(), b.hipCm()),
				Delta.of(fromMetrics.imc(), toMetrics.imc()),
				Delta.of(fromMetrics.tmbKcal(), toMetrics.tmbKcal()),
				Delta.of(fromMetrics.getKcal(), toMetrics.getKcal()));
	}
}
