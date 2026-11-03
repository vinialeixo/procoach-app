package com.fitness.procoach.assessment.interfaces;

import com.fitness.procoach.assessment.application.AssessmentApplicationService;
import com.fitness.procoach.assessment.domain.AssessmentComparison;
import io.swagger.v3.oas.annotations.media.Schema;

/** Duas avaliações do mesmo atleta lado a lado, com a variação de cada medida (T-026). */
@Schema(description = "Comparação de duas avaliações do mesmo atleta. Toda variação é (to - from); "
		+ "campos opcionais ausentes em uma das pontas resultam em variação null.")
public record AssessmentComparisonResponse(
		@Schema(description = "Avaliação de partida") AssessmentResponse from,
		@Schema(description = "Avaliação de chegada") AssessmentResponse to,
		@Schema(description = "Dias entre as duas avaliações (negativo se 'to' é anterior a 'from')",
				example = "56") long daysBetween,
		Delta weightKg, Delta bodyFatPercentage, Delta muscleMassKg, Delta waistCm, Delta hipCm,
		Delta imc, Delta tmbKcal, Delta getKcal) {

	/** Valor nas duas avaliações e a diferença. */
	public record Delta(java.math.BigDecimal from, java.math.BigDecimal to, java.math.BigDecimal change,
			java.math.BigDecimal changePercent) {

		static Delta from(AssessmentComparison.Delta delta) {
			return new Delta(delta.from(), delta.to(), delta.change(), delta.changePercent());
		}
	}

	public static AssessmentComparisonResponse from(AssessmentApplicationService.ComparisonView view) {
		AssessmentComparison c = view.comparison();
		return new AssessmentComparisonResponse(AssessmentResponse.from(view.from()),
				AssessmentResponse.from(view.to()), c.daysBetween(), Delta.from(c.weightKg()),
				Delta.from(c.bodyFatPercentage()), Delta.from(c.muscleMassKg()), Delta.from(c.waistCm()),
				Delta.from(c.hipCm()), Delta.from(c.imc()), Delta.from(c.tmbKcal()), Delta.from(c.getKcal()));
	}
}
