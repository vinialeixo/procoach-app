package com.fitness.procoach.nutrition.domain;

import java.time.Instant;

/**
 * Evento de domínio emitido pelo agregado Diet. Framework-agnostic.
 * Consumido futuramente pelo módulo de auditoria (append-only).
 */
public sealed interface DietEvent permits DietCreated, DietVersioned, DietStatusChanged, DietDeleted {

    DietId dietId();

    Instant occurredAt();
}
