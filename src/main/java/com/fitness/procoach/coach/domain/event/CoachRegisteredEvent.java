package com.fitness.procoach.coach.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado quando um Coach é registrado (vinculado a um User). Insumo para o
 * AuditLog (regra de auditoria de escritas).
 */
public record CoachRegisteredEvent(UUID coachId, UUID userId, String name, Instant occurredAt) {
}
