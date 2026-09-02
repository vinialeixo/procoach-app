package com.fitness.procoach.assessment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento de domínio emitido quando uma avaliação é criada.
 * Base para o registro em AuditLog (regra inegociável 4).
 */
public record AssessmentCreated(AssessmentId assessmentId, UUID athleteId, UUID coachId,
		LocalDate assessmentDate, Instant occurredAt) {

	public static AssessmentCreated of(Assessment assessment) {
		return new AssessmentCreated(assessment.id(), assessment.athleteId(), assessment.coachId(),
				assessment.assessmentDate(), Instant.now());
	}
}
