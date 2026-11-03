package com.fitness.procoach.athlete.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador único e imutável do agregado Athlete (US-002).
 * Gerado automaticamente pelo sistema e nunca reutilizado.
 */
public final class AthleteId {

	private final UUID value;

	private AthleteId(UUID value) {
		this.value = Objects.requireNonNull(value, "AthleteId value must not be null");
	}

	public static AthleteId generate() {
		return new AthleteId(UUID.randomUUID());
	}

	public static AthleteId of(UUID value) {
		return new AthleteId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AthleteId other)) {
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
		return value.toString();
	}
}
