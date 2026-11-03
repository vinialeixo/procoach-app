package com.fitness.procoach.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Comparação lado a lado de duas avaliações (T-026): variação absoluta e percentual. */
class AssessmentComparisonTest {

	private static Assessment assessment(String date, String weight, String bodyFat, String waist) {
		return Assessment.create(UUID.randomUUID(), UUID.randomUUID(), LocalDate.parse(date),
				new BodyMeasurements(new BigDecimal(weight), new BigDecimal("178"),
						bodyFat == null ? null : new BigDecimal(bodyFat), null,
						waist == null ? null : new BigDecimal(waist), null));
	}

	private static AssessmentMetrics metrics(String imc, String tmb, String get) {
		return new AssessmentMetrics(new BigDecimal(imc), new BigDecimal(tmb), new BigDecimal(get), null, null);
	}

	@Test
	void calculaVariacaoAbsolutaEPercentualDeToMenosFrom() {
		Assessment from = assessment("2026-07-01", "90.00", "20.00", "95.00");
		Assessment to = assessment("2026-08-26", "84.60", "16.00", "90.25");

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
		Assessment from = assessment("2026-07-01", "90", null, "95");
		Assessment to = assessment("2026-08-01", "88", "15", null);

		AssessmentComparison c = AssessmentComparison.of(from, metrics("28", "1900", "2280"), to,
				metrics("27", "1850", "2220"));

		assertThat(c.bodyFatPercentage().change()).isNull();
		assertThat(c.bodyFatPercentage().to()).isEqualByComparingTo("15");
		assertThat(c.waistCm().change()).isNull();
		assertThat(c.weightKg().change()).isEqualByComparingTo("-2.00");
	}

	@Test
	void dataInvertidaGeraDiasNegativos() {
		Assessment from = assessment("2026-08-01", "90", null, null);
		Assessment to = assessment("2026-07-01", "92", null, null);

		AssessmentComparison c = AssessmentComparison.of(from, metrics("28", "1900", "2280"), to,
				metrics("29", "1950", "2340"));

		assertThat(c.daysBetween()).isEqualTo(-31);
		assertThat(c.weightKg().change()).isEqualByComparingTo("2.00");
	}
}
