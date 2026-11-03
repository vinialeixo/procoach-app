package com.fitness.procoach.athlete.domain;

import java.util.Objects;

/**
 * Nome do atleta. Não pode ser vazio e tem tamanho máximo definido.
 */
public final class Name {

	private static final int MAX_LENGTH = 150;

	private final String value;

	public Name(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Name must not be blank");
		}
		String trimmed = value.trim();
		if (trimmed.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Name must not exceed " + MAX_LENGTH + " characters");
		}
		this.value = trimmed;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Name other)) {
			return false;
		}
		return value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
