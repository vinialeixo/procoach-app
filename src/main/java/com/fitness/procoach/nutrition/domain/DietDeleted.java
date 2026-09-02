package com.fitness.procoach.nutrition.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitido quando a dieta é logicamente excluída (soft-delete).
 */
public record DietDeleted(DietId dietId, UUID deletedBy, Instant occurredAt) implements DietEvent {
}
