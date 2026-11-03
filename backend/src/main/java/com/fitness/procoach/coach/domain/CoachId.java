package com.fitness.procoach.coach.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador único e imutável do agregado Coach (T-058). Tipo exposto pelo
 * módulo coach (named interface) para que demais módulos tipifiquem suas
 * referências ao Coach — substitui o record local que existia em nutrition.
 */
public final class CoachId {

	private final UUID value;

	private CoachId(UUID value) {
		this.value = Objects.requireNonNull(value, "CoachId value must not be null");
	}

	public static CoachId generate() {
		return new CoachId(UUID.randomUUID());
	}

	public static CoachId of(UUID value) {
		return new CoachId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CoachId other)) {
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
