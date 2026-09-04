package com.fitness.procoach.athlete.application;

import java.util.UUID;

/**
 * Atleta não encontrado — inexistente, removido (soft-delete) ou fora da
 * carteira do coach autenticado. Mapeado para {@code 404 Not Found} para não
 * vazar a existência do recurso.
 */
public class AthleteNotFoundException extends RuntimeException {

	public AthleteNotFoundException(UUID id) {
		super("Athlete not found: " + id);
	}
}
