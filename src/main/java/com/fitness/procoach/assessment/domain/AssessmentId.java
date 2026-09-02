package com.fitness.procoach.assessment.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador imutável do agregado Assessment.
 */
public final class AssessmentId {

	private final UUID value;

	public AssessmentId(UUID value) {
		this.value = Objects.requireNonNull(value, "assessmentId must not be null");
	}

	public static AssessmentId generate() {
		return new AssessmentId(UUID.randomUUID());
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AssessmentId that)) {
			return false;
		}
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
