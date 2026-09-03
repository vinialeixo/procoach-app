package com.fitness.procoach.assessment.application;

import java.util.UUID;

/**
 * Lançada quando uma avaliação não é encontrada.
 */
public class AssessmentNotFoundException extends RuntimeException {

	public AssessmentNotFoundException(UUID id) {
		super("Avaliação não encontrada: " + id);
	}
}
