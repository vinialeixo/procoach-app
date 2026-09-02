package com.fitness.procoach.nutrition.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Referência por UUID ao agregado Coach (bounded context security).
 * Nunca um objeto aninhado — apenas o identificador.
 */
public record CoachId(UUID value) {

    public CoachId {
        Objects.requireNonNull(value, "CoachId value must not be null");
    }

    public static CoachId of(UUID value) {
        return new CoachId(value);
    }
}
