package com.fitness.procoach.nutrition.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raiz do bounded context Nutrition. Representa uma única versão de uma dieta.
 *
 * <p>Regras inegociáveis aplicadas aqui:
 * <ul>
 *   <li>Nunca sobrescrever: cada alteração gera uma nova versão (v1 -> v2 -> v3) com
 *       histórico encadeado via {@code previousVersionId} (US-015).</li>
 *   <li>Versões anteriores tornam-se somente leitura (US-015).</li>
 *   <li>Macronutrientes obrigatórios e positivos (US-014).</li>
 *   <li>Objetivo definido na criação (US-011).</li>
 *   <li>Exclusão lógica (soft-delete), nunca DELETE físico.</li>
 *   <li>Referências a outros agregados por UUID (coachId, athleteId).</li>
 * </ul>
 *
 * <p>Framework-agnostic: nenhuma anotação Spring/JPA neste pacote.
 */
public class Diet {

    private final DietId id;
    private final CoachId coachId;
    private final AthleteId athleteId;
    private DietObjective objective;
    private Macronutrients macros;
    private DietStatus status;
    private final int version;
    private final DietId previousVersionId;
    private final String changeReason;
    private final UUID createdBy;
    private UUID updatedBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private boolean readOnly;
    private boolean deleted;
    private final List<DietEvent> domainEvents = new ArrayList<>();

    private Diet(DietId id, CoachId coachId, AthleteId athleteId, DietObjective objective,
                 Macronutrients macros, DietStatus status, int version, DietId previousVersionId,
                 String changeReason, UUID createdBy, UUID updatedBy, Instant createdAt,
                 Instant updatedAt, boolean readOnly, boolean deleted) {
        this.id = Objects.requireNonNull(id, "id");
        this.coachId = Objects.requireNonNull(coachId, "coachId");
        this.athleteId = Objects.requireNonNull(athleteId, "athleteId");
        this.objective = Objects.requireNonNull(objective, "objective");
        this.macros = Objects.requireNonNull(macros, "macros");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
        this.previousVersionId = previousVersionId;
        this.changeReason = changeReason;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.updatedBy = updatedBy;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt;
        this.readOnly = readOnly;
        this.deleted = deleted;
    }

    /**
     * Cria a primeira versão (v1) de uma dieta, em estado Rascunho.
     */
    public static Diet create(CoachId coachId, AthleteId athleteId, DietObjective objective,
                              Macronutrients macros, UUID createdBy) {
        Instant now = Instant.now();
        Diet diet = new Diet(DietId.random(), coachId, athleteId, objective, macros,
                DietStatus.DRAFT, 1, null, null, createdBy, createdBy, now, now, false, false);
        diet.registerEvent(new DietCreated(diet.id, coachId, athleteId, objective, macros, createdBy, now));
        return diet;
    }

    /**
     * Cria uma nova versão a partir desta. A versão atual torna-se somente leitura.
     * O motivo da alteração é obrigatório (US-016).
     */
    public Diet createNewVersion(DietObjective newObjective, Macronutrients newMacros,
                                 String changeReason, UUID changedBy) {
        if (readOnly) {
            throw new IllegalStateException("Cannot create a new version from a read-only version");
        }
        if (deleted) {
            throw new IllegalStateException("Cannot create a new version from a deleted diet");
        }
        if (changeReason == null || changeReason.isBlank()) {
            throw new IllegalArgumentException("changeReason is required to create a new version");
        }
        Instant now = Instant.now();
        this.readOnly = true;
        this.updatedBy = changedBy;
        this.updatedAt = now;
        Diet next = new Diet(DietId.random(), coachId, athleteId, newObjective, newMacros,
                DietStatus.DRAFT, version + 1, id, changeReason, changedBy, changedBy, now, now, false, false);
        next.registerEvent(new DietVersioned(next.id, id, version + 1, newObjective, newMacros,
                changeReason, changedBy, now));
        return next;
    }

    /**
     * Transiciona o estado da dieta seguindo o fluxo
     * Rascunho -> Em Revisão -> Aprovada -> Arquivada (US-017).
     * Transições inválidas são bloqueadas.
     */
    public void changeStatus(DietStatus newStatus) {
        if (deleted) {
            throw new IllegalStateException("Cannot change status of a deleted diet");
        }
        if (readOnly) {
            throw new IllegalStateException("Cannot change status of a read-only version");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus must not be null");
        }
        if (!isValidTransition(status, newStatus)) {
            throw new IllegalStateException("Invalid status transition: " + status + " -> " + newStatus);
        }
        DietStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedAt = Instant.now();
        registerEvent(new DietStatusChanged(id, oldStatus, newStatus, updatedAt));
    }

    /**
     * Exclusão lógica (soft-delete). Nunca remove fisicamente.
     */
    public void softDelete(UUID deletedBy) {
        if (deleted) {
            return;
        }
        this.deleted = true;
        this.updatedBy = deletedBy;
        this.updatedAt = Instant.now();
        registerEvent(new DietDeleted(id, deletedBy, updatedAt));
    }

    private static boolean isValidTransition(DietStatus from, DietStatus to) {
        return switch (from) {
            case DRAFT -> to == DietStatus.IN_REVIEW;
            case IN_REVIEW -> to == DietStatus.APPROVED;
            case APPROVED -> to == DietStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
    }

    public List<DietEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    private void registerEvent(DietEvent event) {
        domainEvents.add(event);
    }

    public DietId id() {
        return id;
    }

    public CoachId coachId() {
        return coachId;
    }

    public AthleteId athleteId() {
        return athleteId;
    }

    public DietObjective objective() {
        return objective;
    }

    public Macronutrients macros() {
        return macros;
    }

    public DietStatus status() {
        return status;
    }

    public int version() {
        return version;
    }

    public DietId previousVersionId() {
        return previousVersionId;
    }

    public String changeReason() {
        return changeReason;
    }

    public UUID createdBy() {
        return createdBy;
    }

    public UUID updatedBy() {
        return updatedBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
