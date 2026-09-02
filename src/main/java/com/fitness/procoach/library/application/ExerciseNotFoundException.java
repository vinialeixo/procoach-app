package com.fitness.procoach.library.application;

import java.util.UUID;

/** Lançada quando um exercício não é encontrado na biblioteca. */
public class ExerciseNotFoundException extends RuntimeException {

	public ExerciseNotFoundException(UUID id) {
		super("Exercício não encontrado: " + id);
	}
}
