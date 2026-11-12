package com.fitness.procoach.nutrition.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador imutável de um MealItem (entidade interna da Diet).
 */
public record MealItemId(UUID value) {

    public MealItemId {
        Objects.requireNonNull(value, "MealItemId value must not be null");
    }

    public static MealItemId of(UUID value) {
        return new MealItemId(value);
    }

    public static MealItemId random() {
        return new MealItemId(UUID.randomUUID());
    }
}