package com.fitness.procoach.nutrition.domain;

/**
 * Origem do alimento (T-085 / docs/nutricao-plano-alimentar.md §3.4):
 * - TACO: tabela brasileira de composição de alimentos
 * - USDA: base de dados americana
 * - CUSTOM: criado pelo coach (exige coachApproved = true)
 */
public enum FoodSource {
    TACO,
    USDA,
    CUSTOM
}