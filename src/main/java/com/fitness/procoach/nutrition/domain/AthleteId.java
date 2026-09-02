package com.fitness.procoach.nutrition.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Referência por UUID ao agregado Athlete (bounded context athlete).
 * Nunca um objeto aninhado — apenas o identificador.
 */
public record AthleteId(UUID value) {

    public AthleteId {
        Objects.requireNonNull(value, "AthleteId value must not be null");
    }

    public static AthleteId of(UUID value) {
        return new AthleteId(value);
    }
}
