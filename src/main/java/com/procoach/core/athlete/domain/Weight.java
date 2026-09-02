package com.procoach.core.athlete.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Peso do atleta em quilogramas. Deve ser positivo.
 */
public final class Weight {

	private final BigDecimal value;

	public Weight(BigDecimal value) {
		if (value == null) {
			throw new IllegalArgumentException("Weight must not be null");
		}
		if (value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Weight must be positive");
		}
		this.value = value;
	}

	public BigDecimal value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Weight other)) {
			return false;
		}
		return value.compareTo(other.value) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(value.stripTrailingZeros());
	}

	@Override
	public String toString() {
		return value.toPlainString();
	}
}
