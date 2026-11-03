package com.fitness.procoach.nutrition.application;

import java.util.UUID;

/**
 * Lançada quando uma dieta não é encontrada.
 */
public class DietNotFoundException extends RuntimeException {

	public DietNotFoundException(UUID id) {
		super("Dieta não encontrada: " + id);
	}
}
