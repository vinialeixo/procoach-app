package com.fitness.procoach.library.domain;

import java.util.Objects;

/**
 * Referência bibliográfica/científica que embasa as informações do exercício.
 * Value Object imutável. Framework-agnostic.
 */
public record ExerciseReference(String authors, String title, String source, Integer year) {

	public ExerciseReference {
		Objects.requireNonNull(authors, "authors é obrigatório");
		Objects.requireNonNull(title, "title é obrigatório");
		Objects.requireNonNull(source, "source é obrigatório");
		if (year != null && year < 1900) {
			throw new IllegalArgumentException("year inválido: " + year);
		}
	}
}
