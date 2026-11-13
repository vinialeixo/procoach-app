package com.fitness.procoach.nutrition.application;

import java.util.UUID;

/**
 * Lançada quando um alimento não é encontrado, está excluído ou pertence a
 * outro coach (anti-IDOR: não vaza a existência do recurso).
 */
public class FoodNotFoundException extends RuntimeException {

	public FoodNotFoundException(UUID id) {
		super("Alimento não encontrado: " + id);
	}
}
