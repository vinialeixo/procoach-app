package com.fitness.procoach.assessment.interfaces;

import com.fitness.procoach.assessment.domain.Assessment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de saída de uma avaliação física.
 */
public record AssessmentResponse(
		UUID id,
		UUID athleteId,
		UUID coachId,
		LocalDate assessmentDate,
		BigDecimal weightKg,
		BigDecimal heightCm,
		BigDecimal bodyFatPercentage,
		BigDecimal muscleMassKg,
		BigDecimal waistCm,
		BigDecimal hipCm,
		Instant createdAt) {

	public static AssessmentResponse from(Assessment assessment) {
		return new AssessmentResponse(assessment.id().value(), assessment.athleteId(),
				assessment.coachId(), assessment.assessmentDate(),
				assessment.measurements().weightKg(), assessment.measurements().heightCm(),
				assessment.measurements().bodyFatPercentage(), assessment.measurements().muscleMassKg(),
				assessment.measurements().waistCm(), assessment.measurements().hipCm(),
				assessment.createdAt());
	}
}
