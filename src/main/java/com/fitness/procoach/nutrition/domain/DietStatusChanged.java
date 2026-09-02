package com.fitness.procoach.nutrition.domain;

import java.time.Instant;

/**
 * Emitido quando o estado da dieta muda (Rascunho -> Em Revisão -> Aprovada -> Arquivada).
 */
public record DietStatusChanged(DietId dietId, DietStatus from, DietStatus to,
                                Instant occurredAt) implements DietEvent {
}
