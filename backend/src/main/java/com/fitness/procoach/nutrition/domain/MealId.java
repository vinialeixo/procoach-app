package com.fitness.procoach.nutrition.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador imutável de uma Meal (entidade interna da Diet).
 */
public record MealId(UUID value) {

    public MealId {
        Objects.requireNonNull(value, "MealId value must not be null");
    }

    public static MealId of(UUID value) {
        return new MealId(value);
    }

    public static MealId random() {
        return new MealId(UUID.randomUUID());
    }
}