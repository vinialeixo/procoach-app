package com.fitness.procoach.nutrition.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador imutável do agregado Diet.
 */
public record DietId(UUID value) {

    public DietId {
        Objects.requireNonNull(value, "DietId value must not be null");
    }

    public static DietId of(UUID value) {
        return new DietId(value);
    }

    public static DietId random() {
        return new DietId(UUID.randomUUID());
    }
}
