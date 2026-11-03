package com.fitness.procoach.assessment.domain;

import java.math.BigDecimal;

/**
 * Métricas derivadas de uma avaliação (T-025/US-05), calculadas no núcleo — o
 * front não recalcula. {@code weightChange*} é em relação à avaliação anterior
 * do mesmo atleta e fica {@code null} na primeira.
 *
 * @param imc                  índice de massa corporal (kg/m²), 2 casas
 * @param tmbKcal              taxa metabólica basal (Mifflin-St Jeor), kcal/dia
 * @param getKcal              gasto energético total = TMB × fator de atividade, kcal/dia
 * @param weightChangeKg       variação de peso em kg, 2 casas
 * @param weightChangePercent  variação de peso em %, 2 casas
 */
public record AssessmentMetrics(BigDecimal imc, BigDecimal tmbKcal, BigDecimal getKcal,
		BigDecimal weightChangeKg, BigDecimal weightChangePercent) {
}
