package com.fitness.procoach.nutrition.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador imutável do agregado Food (catálogo de alimentos, T-086).
 */
public record FoodId(UUID value) {

    public FoodId {
        Objects.requireNonNull(value, "FoodId value must not be null");
    }

    public static FoodId of(UUID value) {
        return new FoodId(value);
    }

    public static FoodId random() {
        return new FoodId(UUID.randomUUID());
    }
}
