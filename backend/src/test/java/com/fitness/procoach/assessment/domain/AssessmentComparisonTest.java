package com.fitness.procoach.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Comparação lado a lado de duas avaliações (T-026): variação absoluta e percentual. */
class AssessmentComparisonTest {

	private static Assessment assessment(String date, String weight, String bodyFat, String muscleMass,
			String waist, String hip) {
		return Assessment.create(UUID.randomUUID(), UUID.randomUUID(), LocalDate.parse(date),
				new BodyMeasurements(new BigDecimal(weight), new BigDecimal("178"),
						bodyFat == null ? null : new BigDecimal(bodyFat),
						muscleMass == null ? null : new BigDecimal(muscleMass),
						waist == null ? null : new BigDecimal(waist),
						hip == null ? null : new BigDecimal(hip)));
	}

	// Sobrecarga para compatibilidade com testes existentes (sem muscleMass e hip)
	private static Assessment assessment(String date, String weight, String bodyFat, String waist) {
		return assessment(date, weight, bodyFat, null, waist, null);
	}

	private static AssessmentMetrics metrics(String imc, String tmb, String get) {
		return new AssessmentMetrics(new BigDecimal(imc), new BigDecimal(tmb), new BigDecimal(get), null, null);
	}

	@Test
	void calculaVariacaoAbsolutaEPercentualDeToMenosFrom() {
		Assessment from = assessment("2026-07-01", "90.00", "20.00", null, "95.00", null);
		Assessment to = assessment("2026-08-26", "84.60", "16.00", null, "90.25", null);

		AssessmentComparison c = AssessmentComparison.of(from, metrics("28.40", "1900", "2280"), to,
				metrics("26.70", "1800", "2160"));

		assertThat(c.daysBetween()).isEqualTo(56);
		assertThat(c.weightKg().change()).isEqualByComparingTo("-5.40");
		assertThat(c.weightKg().changePercent()).isEqualByComparingTo("-6.00");
		assertThat(c.bodyFatPercentage().change()).isEqualByComparingTo("-4.00");
		assertThat(c.bodyFatPercentage().changePercent()).isEqualByComparingTo("-20.00");
		assertThat(c.waistCm().change()).isEqualByComparingTo("-4.75");
		assertThat(c.imc().change()).isEqualByComparingTo("-1.70");
		assertThat(c.tmbKcal().change()).isEqualByComparingTo("-100.00");
		assertThat(c.getKcal().change()).isEqualByComparingTo("-120.00");
		assertThat(c.weightKg().from()).isEqualByComparingTo("90.00");
		assertThat(c.weightKg().to()).isEqualByComparingTo("84.60");
	}

	@Test
	void medidaOpcionalAusenteEmUmaDasPontasFicaSemVariacao() {
		Assessment from = assessment("2026-07-01", "90", null, null, "95", null);
		Assessment to = assessment("2026-08-01", "88", "15", null, null, null);

		AssessmentComparison c = AssessmentComparison.of(from, metrics("28", "1900", "2280"), to,
				metrics("27", "1850", "2220"));

		assertThat(c.bodyFatPercentage().change()).isNull();
		assertThat(c.bodyFatPercentage().to()).isEqualByComparingTo("15");
		assertThat(c.waistCm().change()).isNull();
		assertThat(c.weightKg().change()).isEqualByComparingTo("-2.00");
	}

	@Test
	void dataInvertidaGeraDiasNegativos() {
		Assessment from = assessment("2026-08-01", "90", null, null, null, null);
		Assessment to = assessment("2026-07-01", "92", null, null, null, null);

		AssessmentComparison c = AssessmentComparison.of(from, metrics("28", "1900", "2280"), to,
				metrics("29", "1950", "2340"));

		assertThat(c.daysBetween()).isEqualTo(-31);
		assertThat(c.weightKg().change()).isEqualByComparingTo("2.00");
	}

	// ===== Testes adicionais para US-009 (comparação lado a lado) =====

	@Test
	void comparaTodasAsMetricasRegistradas() {
		Assessment from = assessment("2026-07-01", "90.00", "20.00", "70.00", "95.00", "96.00");
		Assessment to = assessment("2026-08-26", "84.60", "16.00", "72.50", "90.25", "94.00");

		AssessmentComparison c = AssessmentComparison.of(from, metrics("28.40", "1900", "2280"), to,
				metrics("26.70", "1800", "2160"));

		assertThat(c.daysBetween()).isEqualTo(56);
		assertThat(c.weightKg().change()).isEqualByComparingTo("-5.40");
		assertThat(c.weightKg().changePercent()).isEqualByComparingTo("-6.00");
		assertThat(c.bodyFatPercentage().change()).isEqualByComparingTo("-4.00");
		assertThat(c.bodyFatPercentage().changePercent()).isEqualByComparingTo("-20.00");
		assertThat(c.muscleMassKg().change()).isEqualByComparingTo("2.50");
		assertThat(c.muscleMassKg().changePercent()).isEqualByComparingTo("3.57");
		assertThat(c.waistCm().change()).isEqualByComparingTo("-4.75");
		assertThat(c.hipCm().change()).isEqualByComparingTo("-2.00");
		assertThat(c.imc().change()).isEqualByComparingTo("-1.70");
		assertThat(c.tmbKcal().change()).isEqualByComparingTo("-100.00");
		assertThat(c.getKcal().change()).isEqualByComparingTo("-120.00");
	}

	@Test
	void metricaOpcionalPresenteEmAmbasGeraVariacao() {
		Assessment from = assessment("2026-07-01", "90", "20", "70", "95", "96");
		Assessment to = assessment("2026-08-01", "88", "18", "71", "93", "95");

		AssessmentComparison c = AssessmentComparison.of(from, metrics("28", "1900", "2280"), to,
				metrics("27", "1850", "2220"));

		assertThat(c.bodyFatPercentage().change()).isEqualByComparingTo("-2.00");
		assertThat(c.bodyFatPercentage().changePercent()).isEqualByComparingTo("-10.00");
		assertThat(c.muscleMassKg().change()).isEqualByComparingTo("1.00");
		assertThat(c.waistCm().change()).isEqualByComparingTo("-2.00");
		assertThat(c.hipCm().change()).isEqualByComparingTo("-1.00");
	}

	@Test
	void metricaOpcionalAusenteEmAmbasFicaSemVariacao() {
		Assessment from = assessment("2026-07-01", "90", null, null, "95", null);
		Assessment to = assessment("2026-08-01", "88", null, null, "93", null);

		AssessmentComparison c = AssessmentComparison.of(from, metrics("28", "1900", "2280"), to,
				metrics("27", "1850", "2220"));

		assertThat(c.bodyFatPercentage().change()).isNull();
		assertThat(c.muscleMassKg().change()).isNull();
		assertThat(c.hipCm().change()).isNull();
		assertThat(c.weightKg().change()).isEqualByComparingTo("-2.00");
		assertThat(c.waistCm().change()).isEqualByComparingTo("-2.00");
	}

	@Test
	void variacaoDeImcTmbGetDerivadasDasMedidas() {
		// from: 90kg, 178cm -> IMC = 28.40, TMB ~1900, GET ~2280
		// to:   84.6kg, 178cm -> IMC = 26.70, TMB ~1800, GET ~2160
		Assessment from = assessment("2026-07-01", "90.00", null, null, null, null);
		Assessment to = assessment("2026-08-26", "84.60", null, null, null, null);

		AssessmentComparison c = AssessmentComparison.of(from, metrics("28.40", "1900", "2280"), to,
				metrics("26.70", "1800", "2160"));

		assertThat(c.imc().from()).isEqualByComparingTo("28.40");
		assertThat(c.imc().to()).isEqualByComparingTo("26.70");
		assertThat(c.imc().change()).isEqualByComparingTo("-1.70");
		assertThat(c.tmbKcal().change()).isEqualByComparingTo("-100.00");
		assertThat(c.getKcal().change()).isEqualByComparingTo("-120.00");
	}

	@Test
	void ordemDasAvaliacoesDefineSinalDaVariacao() {
		// from mais antigo (peso maior) -> to mais novo (peso menor) = perda de peso (negativo)
		Assessment from = assessment("2026-07-01", "90", null, null, null, null);
		Assessment to = assessment("2026-08-01", "85", null, null, null, null);

		AssessmentComparison c1 = AssessmentComparison.of(from, metrics("28", "1900", "2280"), to,
				metrics("26", "1800", "2160"));

		assertThat(c1.weightKg().change()).isEqualByComparingTo("-5.00");

		// Invertido: from mais novo -> to mais antigo = ganho de peso (positivo)
		AssessmentComparison c2 = AssessmentComparison.of(to, metrics("26", "1800", "2160"), from,
				metrics("28", "1900", "2280"));

		assertThat(c2.weightKg().change()).isEqualByComparingTo("5.00");
	}
}