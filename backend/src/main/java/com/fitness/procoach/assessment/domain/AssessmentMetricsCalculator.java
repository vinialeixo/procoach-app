package com.fitness.procoach.assessment.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * Motor de cálculos da avaliação física (T-025/US-05): IMC, TMB, GET e evolução
 * de peso. Funções puras, sem estado e sem framework.
 *
 * <p>TMB pela equação de Mifflin-St Jeor:
 * {@code 10·peso(kg) + 6,25·altura(cm) − 5·idade + (5 homem | −161 mulher)}.
 * O GET multiplica a TMB por um fator de atividade; enquanto o atleta não tem
 * esse dado cadastrado, usa-se {@link #DEFAULT_ACTIVITY_FACTOR} (sedentário).</p>
 */
public final class AssessmentMetricsCalculator {

	/** Fator de atividade padrão (sedentário) até existir o dado por atleta. */
	public static final BigDecimal DEFAULT_ACTIVITY_FACTOR = new BigDecimal("1.2");

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private AssessmentMetricsCalculator() {
	}

	/**
	 * @param measurements      medidas da avaliação (peso e altura obrigatórios)
	 * @param assessmentDate    data da avaliação, usada para calcular a idade
	 * @param male              sexo biológico do atleta
	 * @param birthDate         data de nascimento do atleta
	 * @param previousWeightKg  peso da avaliação anterior, ou {@code null} se não houver
	 */
	public static AssessmentMetrics calculate(BodyMeasurements measurements, LocalDate assessmentDate,
			boolean male, LocalDate birthDate, BigDecimal previousWeightKg) {
		BigDecimal weight = measurements.weightKg();
		BigDecimal height = measurements.heightCm();

		BigDecimal heightM = height.divide(HUNDRED, 6, RoundingMode.HALF_UP);
		BigDecimal imc = weight.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);

		int age = Math.max(0, Period.between(birthDate, assessmentDate).getYears());
		BigDecimal tmb = weight.multiply(BigDecimal.TEN)
				.add(height.multiply(new BigDecimal("6.25")))
				.subtract(BigDecimal.valueOf(5L * age))
				.add(BigDecimal.valueOf(male ? 5 : -161));
		BigDecimal get = tmb.multiply(DEFAULT_ACTIVITY_FACTOR);

		BigDecimal changeKg = null;
		BigDecimal changePercent = null;
		if (previousWeightKg != null && previousWeightKg.signum() > 0) {
			BigDecimal delta = weight.subtract(previousWeightKg);
			changeKg = delta.setScale(2, RoundingMode.HALF_UP);
			changePercent = delta.divide(previousWeightKg, 6, RoundingMode.HALF_UP)
					.multiply(HUNDRED)
					.setScale(2, RoundingMode.HALF_UP);
		}
		return new AssessmentMetrics(imc, tmb.setScale(0, RoundingMode.HALF_UP),
				get.setScale(0, RoundingMode.HALF_UP), changeKg, changePercent);
	}
}
