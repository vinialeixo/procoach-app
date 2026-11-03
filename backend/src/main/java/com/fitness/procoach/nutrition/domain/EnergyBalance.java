package com.fitness.procoach.nutrition.domain;

import java.math.BigDecimal;

/**
 * Balanço energético de uma dieta em relação ao gasto energético total do atleta
 * (T-030/US-07). Negativo = déficit, positivo = superávit.
 *
 * @param getKcal         gasto energético total usado no cálculo (kcal/dia)
 * @param balanceKcal     calorias da dieta menos o GET (kcal/dia)
 * @param balancePercent  balanço em % do GET, 2 casas
 * @param classification  déficit, manutenção ou superávit
 */
public record EnergyBalance(BigDecimal getKcal, BigDecimal balanceKcal, BigDecimal balancePercent,
		Classification classification) {

	public enum Classification {
		DEFICIT,
		MAINTENANCE,
		SURPLUS
	}
}
