package com.fitness.procoach.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AssessmentMetricsCalculatorTest {

	private static final LocalDate ASSESSMENT_DATE = LocalDate.of(2025, 6, 15);

	private static BodyMeasurements measurements(String weightKg, String heightCm) {
		return new BodyMeasurements(new BigDecimal(weightKg), new BigDecimal(heightCm), null, null, null, null);
	}

	@Test
	void calculaImcTmbEGetDeHomem() {
		// 30 anos exatos: TMB = 10·80 + 6,25·180 − 5·30 + 5 = 1780; GET = 1780 × 1,2 = 2136.
		AssessmentMetrics metrics = AssessmentMetricsCalculator.calculate(measurements("80", "180"),
				ASSESSMENT_DATE, true, LocalDate.of(1995, 6, 15), null);

		assertThat(metrics.imc()).isEqualByComparingTo("24.69");
		assertThat(metrics.tmbKcal()).isEqualByComparingTo("1780");
		assertThat(metrics.getKcal()).isEqualByComparingTo("2136");
	}

	@Test
	void calculaTmbEGetDeMulher() {
		// 28 anos: TMB = 10·60 + 6,25·165 − 5·28 − 161 = 1330,25 -> 1330; GET = 1596,3 -> 1596.
		AssessmentMetrics metrics = AssessmentMetricsCalculator.calculate(measurements("60", "165"),
				ASSESSMENT_DATE, false, LocalDate.of(1997, 6, 15), null);

		assertThat(metrics.imc()).isEqualByComparingTo("22.04");
		assertThat(metrics.tmbKcal()).isEqualByComparingTo("1330");
		assertThat(metrics.getKcal()).isEqualByComparingTo("1596");
	}

	@Test
	void idadeContaAniversarioAindaNaoOcorrido() {
		// Um dia antes do 30º aniversário o atleta ainda tem 29 anos (+5 kcal na TMB).
		AssessmentMetrics metrics = AssessmentMetricsCalculator.calculate(measurements("80", "180"),
				ASSESSMENT_DATE, true, LocalDate.of(1995, 6, 16), null);

		assertThat(metrics.tmbKcal()).isEqualByComparingTo("1785");
	}

	@Test
	void primeiraAvaliacaoNaoTemVariacaoDePeso() {
		AssessmentMetrics metrics = AssessmentMetricsCalculator.calculate(measurements("80", "180"),
				ASSESSMENT_DATE, true, LocalDate.of(1995, 6, 15), null);

		assertThat(metrics.weightChangeKg()).isNull();
		assertThat(metrics.weightChangePercent()).isNull();
	}

	@Test
	void calculaVariacaoDePesoEmRelacaoAAnterior() {
		AssessmentMetrics metrics = AssessmentMetricsCalculator.calculate(measurements("80", "180"),
				ASSESSMENT_DATE, true, LocalDate.of(1995, 6, 15), new BigDecimal("82"));

		assertThat(metrics.weightChangeKg()).isEqualByComparingTo("-2.00");
		assertThat(metrics.weightChangePercent()).isEqualByComparingTo("-2.44");
	}

	@Test
	void ganhoDePesoTemSinalPositivo() {
		AssessmentMetrics metrics = AssessmentMetricsCalculator.calculate(measurements("84", "180"),
				ASSESSMENT_DATE, true, LocalDate.of(1995, 6, 15), new BigDecimal("80"));

		assertThat(metrics.weightChangeKg()).isEqualByComparingTo("4.00");
		assertThat(metrics.weightChangePercent()).isEqualByComparingTo("5.00");
	}

	@Test
	void dataDeAvaliacaoAntesDoNascimentoNaoQuebra() {
		AssessmentMetrics metrics = AssessmentMetricsCalculator.calculate(measurements("80", "180"),
				LocalDate.of(1990, 1, 1), true, LocalDate.of(1995, 6, 15), null);

		// Idade limitada a 0: TMB = 800 + 1125 + 5 = 1930.
		assertThat(metrics.tmbKcal()).isEqualByComparingTo("1930");
	}
}
