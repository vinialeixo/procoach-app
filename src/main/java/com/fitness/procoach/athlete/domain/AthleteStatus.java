package com.fitness.procoach.athlete.domain;

/**
 * Estado do acompanhamento do atleta (US-005). A gestão de transições
 * de estado é tratada em task posterior (T-021).
 */
public enum AthleteStatus {
	ACTIVE,
	PAUSED,
	FINISHED
}
