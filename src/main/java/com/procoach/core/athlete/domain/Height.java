package com.procoach.core.athlete.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Altura do atleta em centímetros. Deve ser positiva.
 */
public final class Height {

	private final BigDecimal value;

	public Height(BigDecimal value) {
		if (value == null) {
			throw new IllegalArgumentException("Height must not be null");
		}
		if (value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Height must be positive");
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
		if (!(o instanceof Height other)) {
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
