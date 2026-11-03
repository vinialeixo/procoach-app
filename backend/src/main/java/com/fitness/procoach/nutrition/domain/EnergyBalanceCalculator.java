package com.fitness.procoach.nutrition.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo do déficit/superávit calórico de uma dieta (T-030/US-07). Função pura:
 * o GET vem do módulo de avaliação (T-025); aqui só se compara com as calorias.
 *
 * <p>Dentro de ±{@link #MAINTENANCE_THRESHOLD_PERCENT}% do GET a dieta é de
 * manutenção; fora disso, déficit (abaixo) ou superávit (acima).</p>
 */
public final class EnergyBalanceCalculator {

	/** Faixa (em % do GET) considerada manutenção. */
	public static final BigDecimal MAINTENANCE_THRESHOLD_PERCENT = new BigDecimal("5");

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private EnergyBalanceCalculator() {
	}

	public static EnergyBalance calculate(double dietCalories, BigDecimal getKcal) {
		if (getKcal == null || getKcal.signum() <= 0) {
			throw new IllegalArgumentException("getKcal must be greater than zero");
		}
		BigDecimal balance = BigDecimal.valueOf(dietCalories).subtract(getKcal).setScale(0, RoundingMode.HALF_UP);
		BigDecimal percent = balance.divide(getKcal, 6, RoundingMode.HALF_UP)
				.multiply(HUNDRED)
				.setScale(2, RoundingMode.HALF_UP);
		EnergyBalance.Classification classification;
		if (percent.abs().compareTo(MAINTENANCE_THRESHOLD_PERCENT) <= 0) {
			classification = EnergyBalance.Classification.MAINTENANCE;
		}
		else if (percent.signum() < 0) {
			classification = EnergyBalance.Classification.DEFICIT;
		}
		else {
			classification = EnergyBalance.Classification.SURPLUS;
		}
		return new EnergyBalance(getKcal, balance, percent, classification);
	}
}
