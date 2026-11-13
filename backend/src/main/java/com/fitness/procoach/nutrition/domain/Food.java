package com.fitness.procoach.nutrition.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado do catálogo de alimentos (T-086 / docs/nutricao-plano-alimentar.md §3.4).
 *
 * <p>Regras inegociáveis aplicadas aqui:
 * <ul>
 *   <li>Alimentos globais (TACO/USDA) não pertencem a nenhum coach; alimentos
 *       {@code CUSTOM} são criados pelo coach e ficam vinculados ao seu
 *       {@code coachId} — nunca visíveis para outro coach.</li>
 *   <li>Alimento {@code CUSTOM} só existe aprovado pelo coach
 *       ({@code coachApproved = true}).</li>
 *   <li>Macros são por 100 g e aceitam valores zerados (T-087 cuida dos
 *       warnings da dieta; o catálogo precisa representar alimentos reais).</li>
 *   <li>Referências entre agregados são por UUID (coachId).</li>
 *   <li>Exclusões são lógicas: {@code deleted} nunca remove a linha.</li>
 * </ul>
 *
 * <p>Framework-agnostic: nenhuma anotação Spring/JPA.
 */
public class Food {

    private final FoodId id;
    private final String tacoId;
    private final String name;
    private final String category;
    private final FoodMacros macrosPer100g;
    private final double ediblePortionPercent;
    private final String householdMeasure;
    private final Double householdMeasureGrams;
    private final FoodSource source;
    private final FoodState stateDefault;
    private final UUID coachId;
    private final boolean coachApproved;
    private final boolean deleted;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Food(FoodId id, String tacoId, String name, String category, FoodMacros macrosPer100g,
                 double ediblePortionPercent, String householdMeasure, Double householdMeasureGrams,
                 FoodSource source, FoodState stateDefault, UUID coachId, boolean coachApproved,
                 boolean deleted, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.tacoId = tacoId;
        this.name = requireText(name, "name", 200);
        this.category = category == null || category.isBlank() ? null : category.trim();
        this.macrosPer100g = Objects.requireNonNull(macrosPer100g, "macrosPer100g");
        if (ediblePortionPercent <= 0 || ediblePortionPercent > 100) {
            throw new IllegalArgumentException("ediblePortionPercent must be between 0 (exclusive) and 100");
        }
        this.ediblePortionPercent = ediblePortionPercent;
        this.householdMeasure = householdMeasure == null || householdMeasure.isBlank()
                ? null : householdMeasure.trim();
        if (householdMeasureGrams != null && householdMeasureGrams <= 0) {
            throw new IllegalArgumentException("householdMeasureGrams must be positive when informed");
        }
        this.householdMeasureGrams = householdMeasureGrams;
        this.source = Objects.requireNonNull(source, "source");
        this.stateDefault = Objects.requireNonNull(stateDefault, "stateDefault");
        if (source == FoodSource.CUSTOM) {
            this.coachId = Objects.requireNonNull(coachId, "coachId is required for CUSTOM foods");
            if (!coachApproved) {
                throw new IllegalArgumentException("CUSTOM foods require coachApproved = true");
            }
        } else {
            if (coachId != null) {
                throw new IllegalArgumentException("Global catalog foods must not have a coachId");
            }
            this.coachId = null;
        }
        this.coachApproved = coachApproved;
        this.deleted = deleted;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Cria um alimento {@code CUSTOM} em nome do coach autenticado. O coach é
     * sempre aprovador; o vínculo com o coachId impede que outro coach o veja.
     */
    public static Food createCustom(UUID coachId, String name, String category, FoodMacros macrosPer100g,
                                    double ediblePortionPercent, String householdMeasure,
                                    Double householdMeasureGrams, FoodState stateDefault) {
        Instant now = Instant.now();
        return new Food(FoodId.random(), null, name, category, macrosPer100g, ediblePortionPercent,
                householdMeasure, householdMeasureGrams, FoodSource.CUSTOM, stateDefault,
                coachId, true, false, now, now);
    }

    /**
     * Reconstrói a partir do estado persistido (uso exclusivo da infraestrutura).
     */
    public static Food reconstitute(FoodId id, String tacoId, String name, String category,
                                    FoodMacros macrosPer100g, double ediblePortionPercent,
                                    String householdMeasure, Double householdMeasureGrams,
                                    FoodSource source, FoodState stateDefault, UUID coachId,
                                    boolean coachApproved, boolean deleted, Instant createdAt,
                                    Instant updatedAt) {
        return new Food(id, tacoId, name, category, macrosPer100g, ediblePortionPercent,
                householdMeasure, householdMeasureGrams, source, stateDefault, coachId,
                coachApproved, deleted, createdAt, updatedAt);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " must have at most " + maxLength + " characters");
        }
        return trimmed;
    }

    public FoodId id() {
        return id;
    }

    public String tacoId() {
        return tacoId;
    }

    public String name() {
        return name;
    }

    public String category() {
        return category;
    }

    public FoodMacros macrosPer100g() {
        return macrosPer100g;
    }

    public double ediblePortionPercent() {
        return ediblePortionPercent;
    }

    public String householdMeasure() {
        return householdMeasure;
    }

    public Double householdMeasureGrams() {
        return householdMeasureGrams;
    }

    public FoodSource source() {
        return source;
    }

    public FoodState stateDefault() {
        return stateDefault;
    }

    public UUID coachId() {
        return coachId;
    }

    public boolean isCustom() {
        return source == FoodSource.CUSTOM;
    }

    public boolean coachApproved() {
        return coachApproved;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
