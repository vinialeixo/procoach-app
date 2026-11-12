package com.fitness.procoach.nutrition.domain;

/**
 * Tipo de item alimentar (US-014/T-085): alimento regular ou suplemento calórico.
 * Suplementos não calóricos (creatina, multivitamínico) ficam em {@code supplements[]} fora das refeições.
 */
public enum FoodKind {
    FOOD,
    SUPPLEMENT_CALORIC
}