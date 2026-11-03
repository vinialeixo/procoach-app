package com.fitness.procoach.nutrition.domain;

import com.fitness.procoach.coach.domain.CoachId;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitido quando a primeira versão de uma dieta é criada.
 */
public record DietCreated(DietId dietId, CoachId coachId, AthleteId athleteId,
                          DietObjective objective, Macronutrients macros,
                          UUID createdBy, Instant occurredAt) implements DietEvent {
}
