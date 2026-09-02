package com.fitness.procoach.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AssessmentTest {

	private static final UUID ATHLETE_ID = UUID.randomUUID();
	private static final UUID COACH_ID = UUID.randomUUID();

	private static BodyMeasurements measurements() {
		return new BodyMeasurements(new BigDecimal("80.5"), new BigDecimal("178.0"),
				new BigDecimal("15.0"), new BigDecimal("35.0"), new BigDecimal("82.0"),
				new BigDecimal("95.0"));
	}

	@Test
	void criaAvaliacaoComDataObrigatoria() {
		LocalDate data = LocalDate.of(2026, 9, 1);

		Assessment assessment = Assessment.create(ATHLETE_ID, COACH_ID, data, measurements());

		assertThat(assessment.id()).isNotNull();
		assertThat(assessment.athleteId()).isEqualTo(ATHLETE_ID);
		assertThat(assessment.coachId()).isEqualTo(COACH_ID);
		assertThat(assessment.assessmentDate()).isEqualTo(data);
		assertThat(assessment.version()).isEqualTo(1L);
	}

	@Test
	void rejeitaAvaliacaoSemData() {
		assertThatThrownBy(() -> Assessment.create(ATHLETE_ID, COACH_ID, null, measurements()))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("assessmentDate");
	}

	@Test
	void rejeitaReferenciasNulas() {
		LocalDate data = LocalDate.of(2026, 9, 1);
		assertThatThrownBy(() -> Assessment.create(null, COACH_ID, data, measurements()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Assessment.create(ATHLETE_ID, null, data, measurements()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Assessment.create(ATHLETE_ID, COACH_ID, data, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void cadaAvaliacaoGeraNovoRegistroSemSobrescrever() {
		LocalDate data = LocalDate.of(2026, 9, 1);

		Assessment primeira = Assessment.create(ATHLETE_ID, COACH_ID, data, measurements());
		Assessment segunda = Assessment.create(ATHLETE_ID, COACH_ID, data, measurements());

		assertThat(primeira.id()).isNotEqualTo(segunda.id());
		assertThat(primeira.assessmentDate()).isEqualTo(segunda.assessmentDate());
		assertThat(primeira.measurements()).isEqualTo(segunda.measurements());
	}

	@Test
	void agregadoEhImutavel() {
		Assessment assessment = Assessment.create(ATHLETE_ID, COACH_ID, LocalDate.of(2026, 9, 1), measurements());

		assertThat(assessment.measurements().weightKg()).isEqualByComparingTo("80.5");
		assertThat(assessment.measurements().heightCm()).isEqualByComparingTo("178.0");
		assertThat(assessment.measurements().bodyFatPercentage()).isEqualByComparingTo("15.0");
	}
}
