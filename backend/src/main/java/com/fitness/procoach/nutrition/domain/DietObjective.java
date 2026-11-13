package com.fitness.procoach.nutrition.domain;

/**
 * Objetivo da dieta (US-011 / T-087): unifica taxonomia com avaliação do atleta.
 * <p>
 * Valores alinhados com o contrato JSON v1 (docs/nutricao-plano-alimentar.md §4):
 * CUTTING, HYPERTROPHY, MAINTENANCE, BULKING, WEIGHT_LOSS, RECOMPOSITION.
 */
public enum DietObjective {
    CUTTING,
    HYPERTROPHY,
    MAINTENANCE,
    BULKING,
    WEIGHT_LOSS,
    RECOMPOSITION
}
