package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.application.DietApplicationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Balanço energético de uma dieta (T-030/US-07): compara as calorias prescritas
 * com o gasto energético total (GET) da avaliação mais recente do atleta.
 */
@Schema(description = "Déficit/superávit calórico da dieta em relação ao GET da avaliação mais recente do atleta.")
public record EnergyBalanceResponse(
		@Schema(description = "Identificador da dieta") UUID dietId,
		@Schema(description = "Calorias diárias da dieta (kcal)", example = "1400.0") double calories,
		@Schema(description = "Taxa metabólica basal (kcal/dia)", example = "1335") BigDecimal tmbKcal,
		@Schema(description = "Gasto energético total (kcal/dia)", example = "1602") BigDecimal getKcal,
		@Schema(description = "Calorias da dieta menos o GET (kcal/dia); negativo = déficit",
				example = "-202") BigDecimal balanceKcal,
		@Schema(description = "Balanço em % do GET", example = "-12.61") BigDecimal balancePercent,
		@Schema(description = "DEFICIT, MAINTENANCE (±5% do GET) ou SURPLUS", example = "DEFICIT") String classification,
		@Schema(description = "Data da avaliação usada no cálculo", example = "2026-09-01") LocalDate basedOnAssessmentDate,
		@Schema(description = "Peso da avaliação usada no cálculo (kg)", example = "61.0") BigDecimal weightKg) {

	public static EnergyBalanceResponse from(DietApplicationService.DietEnergyBalance result) {
		return new EnergyBalanceResponse(result.diet().id().value(), result.diet().macros().calories(),
				result.profile().tmbKcal(), result.balance().getKcal(), result.balance().balanceKcal(),
				result.balance().balancePercent(), result.balance().classification().name(),
				result.profile().assessmentDate(), result.profile().weightKg());
	}
}
