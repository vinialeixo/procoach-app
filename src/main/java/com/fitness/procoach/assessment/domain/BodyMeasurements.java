package com.fitness.procoach.assessment.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Medidas corporais registradas em uma avaliação física.
 * Value object imutável e framework-agnostic.
 */
public final class BodyMeasurements {

	private final BigDecimal weightKg;
	private final BigDecimal heightCm;
	private final BigDecimal bodyFatPercentage;
	private final BigDecimal muscleMassKg;
	private final BigDecimal waistCm;
	private final BigDecimal hipCm;

	public BodyMeasurements(BigDecimal weightKg, BigDecimal heightCm, BigDecimal bodyFatPercentage,
			BigDecimal muscleMassKg, BigDecimal waistCm, BigDecimal hipCm) {
		this.weightKg = requirePositive(weightKg, "weightKg");
		this.heightCm = requirePositive(heightCm, "heightCm");
		this.bodyFatPercentage = bodyFatPercentage;
		this.muscleMassKg = muscleMassKg;
		this.waistCm = waistCm;
		this.hipCm = hipCm;
	}

	private static BigDecimal requirePositive(BigDecimal value, String field) {
		Objects.requireNonNull(value, field + " must not be null");
		if (value.signum() <= 0) {
			throw new IllegalArgumentException(field + " must be greater than zero");
		}
		return value;
	}

	public BigDecimal weightKg() {
		return weightKg;
	}

	public BigDecimal heightCm() {
		return heightCm;
	}

	public BigDecimal bodyFatPercentage() {
		return bodyFatPercentage;
	}

	public BigDecimal muscleMassKg() {
		return muscleMassKg;
	}

	public BigDecimal waistCm() {
		return waistCm;
	}

	public BigDecimal hipCm() {
		return hipCm;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BodyMeasurements that)) {
			return false;
		}
		return weightKg.compareTo(that.weightKg) == 0
				&& heightCm.compareTo(that.heightCm) == 0
				&& compareNullable(bodyFatPercentage, that.bodyFatPercentage)
				&& compareNullable(muscleMassKg, that.muscleMassKg)
				&& compareNullable(waistCm, that.waistCm)
				&& compareNullable(hipCm, that.hipCm);
	}

	private static boolean compareNullable(BigDecimal a, BigDecimal b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.compareTo(b) == 0;
	}

	@Override
	public int hashCode() {
		int result = weightKg.stripTrailingZeros().hashCode();
		result = 31 * result + heightCm.stripTrailingZeros().hashCode();
		result = 31 * result + (bodyFatPercentage != null ? bodyFatPercentage.stripTrailingZeros().hashCode() : 0);
		result = 31 * result + (muscleMassKg != null ? muscleMassKg.stripTrailingZeros().hashCode() : 0);
		result = 31 * result + (waistCm != null ? waistCm.stripTrailingZeros().hashCode() : 0);
		result = 31 * result + (hipCm != null ? hipCm.stripTrailingZeros().hashCode() : 0);
		return result;
	}
}
