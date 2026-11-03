package com.fitness.procoach.coach.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado quando um atleta sai da carteira de um coach
 * ({@code Coach.removeAthlete()} → soft-delete do Athlete). Insumo para o
 * AuditLog.
 */
public record AthleteRemovedFromRosterEvent(UUID coachId, UUID athleteId, Instant occurredAt) {
}
