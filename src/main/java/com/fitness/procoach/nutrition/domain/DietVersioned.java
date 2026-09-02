package com.fitness.procoach.nutrition.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitido quando uma nova versão da dieta é criada (v1 -> v2 -> v3).
 * Mantém a referência à versão anterior (histórico encadeado).
 */
public record DietVersioned(DietId dietId, DietId previousVersionId, int version,
                            DietObjective objective, Macronutrients macros,
                            String changeReason, UUID changedBy, Instant occurredAt) implements DietEvent {
}
