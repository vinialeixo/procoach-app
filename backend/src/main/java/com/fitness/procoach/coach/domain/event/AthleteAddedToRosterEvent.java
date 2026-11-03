package com.fitness.procoach.coach.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado quando um atleta entra na carteira de um coach
 * ({@code Coach.addAthlete()} — UML 12.2). Insumo para o AuditLog.
 */
public record AthleteAddedToRosterEvent(UUID coachId, UUID athleteId, Instant occurredAt) {
}
