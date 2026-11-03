package com.fitness.procoach.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class BodyMeasurementsTest {

	@Test
	void aceitaMedidasValidas() {
		BodyMeasurements m = new BodyMeasurements(new BigDecimal("80.5"), new BigDecimal("178.0"),
				new BigDecimal("15.0"), new BigDecimal("35.0"), new BigDecimal("82.0"), new BigDecimal("95.0"));

		assertThat(m.weightKg()).isEqualByComparingTo("80.5");
		assertThat(m.heightCm()).isEqualByComparingTo("178.0");
		assertThat(m.bodyFatPercentage()).isEqualByComparingTo("15.0");
	}

	@Test
	void rejeitaPesoNuloOuNaoPositivo() {
		assertThatThrownBy(() -> new BodyMeasurements(null, new BigDecimal("178.0"), null, null, null, null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new BodyMeasurements(BigDecimal.ZERO, new BigDecimal("178.0"), null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("weightKg");
	}

	@Test
	void rejeitaAlturaNulaOuNaoPositiva() {
		assertThatThrownBy(() -> new BodyMeasurements(new BigDecimal("80.5"), null, null, null, null, null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new BodyMeasurements(new BigDecimal("80.5"), BigDecimal.ZERO, null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("heightCm");
	}
}
