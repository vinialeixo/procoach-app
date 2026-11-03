package com.fitness.procoach.training.application;

/**
 * Recurso do módulo training (treino, plano ou periodização) inexistente — ou
 * que não pertence ao atleta informado. Vira 404 sem vazar a existência.
 */
public class TrainingResourceNotFoundException extends RuntimeException {

	public TrainingResourceNotFoundException(String message) {
		super(message);
	}

}
