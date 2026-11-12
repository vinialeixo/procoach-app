package com.fitness.procoach.nutrition.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Item de uma refeição dentro de uma dieta versionada (T-085).
 *
 * <p>Regras inegociáveis aplicadas aqui:
 * <ul>
 *   <li>Macros são <b>snapshot</b> no momento da prescrição: mudança no catálogo não altera dietas já criadas.</li>
 *   <li>Referência ao catálogo (foodId) é opcional para alimentos CUSTOM, obrigatória para TACO/USDA.</li>
 *   <li>quantityG é sempre o peso cru / como comprado (RAW) ou como consumido (AS_SERVED).</li>
 * </ul>
 *
 * <p>Framework-agnostic: nenhuma anotação Spring/JPA.
 */
public class MealItem {

    private final MealItemId id;
    private final String foodId;
    private final String name;
    private final FoodSource source;
    private final FoodKind kind;
    private final FoodState state;
    private final double quantityG;
    private final String householdMeasure;
    private final String preparation;
    private final Macronutrients macrosSnapshot;

    private MealItem(MealItemId id, String foodId, String name, FoodSource source,
                     FoodKind kind, FoodState state, double quantityG,
                     String householdMeasure, String preparation,
                     Macronutrients macrosSnapshot) {
        this.id = Objects.requireNonNull(id, "id");
        this.foodId = foodId; // pode ser null para CUSTOM criado inline
        this.name = Objects.requireNonNull(name, "name");
        this.source = Objects.requireNonNull(source, "source");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.state = Objects.requireNonNull(state, "state");
        if (quantityG <= 0) {
            throw new IllegalArgumentException("quantityG must be positive");
        }
        this.quantityG = quantityG;
        this.householdMeasure = householdMeasure; // opcional
        this.preparation = preparation; // opcional
        this.macrosSnapshot = Objects.requireNonNull(macrosSnapshot, "macrosSnapshot");
    }

    /**
     * Cria um novo item de refeição.
     */
    public static MealItem create(String foodId, String name, FoodSource source,
                                  FoodKind kind, FoodState state, double quantityG,
                                  String householdMeasure, String preparation,
                                  Macronutrients macrosSnapshot) {
        return new MealItem(MealItemId.random(), foodId, name, source, kind, state,
                quantityG, householdMeasure, preparation, macrosSnapshot);
    }

    /**
     * Reconstrói a partir do estado persistido (uso exclusivo da infraestrutura).
     */
    public static MealItem reconstitute(MealItemId id, String foodId, String name,
                                        FoodSource source, FoodKind kind, FoodState state,
                                        double quantityG, String householdMeasure,
                                        String preparation, Macronutrients macrosSnapshot) {
        return new MealItem(id, foodId, name, source, kind, state, quantityG,
                householdMeasure, preparation, macrosSnapshot);
    }

    public MealItemId id() {
        return id;
    }

    public String foodId() {
        return foodId;
    }

    public String name() {
        return name;
    }

    public FoodSource source() {
        return source;
    }

    public FoodKind kind() {
        return kind;
    }

    public FoodState state() {
        return state;
    }

    public double quantityG() {
        return quantityG;
    }

    public String householdMeasure() {
        return householdMeasure;
    }

    public String preparation() {
        return preparation;
    }

    public Macronutrients macrosSnapshot() {
        return macrosSnapshot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MealItem that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "MealItem[id=" + id + ", name=" + name + ", quantityG=" + quantityG
                + ", macros=" + macrosSnapshot + "]";
    }
}