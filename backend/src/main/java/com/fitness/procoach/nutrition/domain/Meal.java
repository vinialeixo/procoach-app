package com.fitness.procoach.nutrition.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Refeição dentro de uma dieta versionada (T-085).
 *
 * <p>Uma refeição contém itens (MealItem) e tem metas opcionais por refeição.
 * Os totais da refeição são derivados dos itens (nunca digitados manualmente).
 *
 * <p>Framework-agnostic: nenhuma anotação Spring/JPA.
 */
public class Meal {

    private final MealId id;
    private final String name;
    private final int order;
    private final String time; // formato HH:mm, opcional
    private final String notes; // opcional
    private final Macronutrients target; // metas por refeição, opcional
    private final List<MealItem> items;

    private Meal(MealId id, String name, int order, String time, String notes,
                 Macronutrients target, List<MealItem> items) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        if (order < 0) {
            throw new IllegalArgumentException("order must be non-negative");
        }
        this.order = order;
        this.time = time; // opcional
        this.notes = notes; // opcional
        this.target = target; // opcional
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    /**
     * Cria uma nova refeição.
     */
    public static Meal create(String name, int order, String time, String notes,
                              Macronutrients target) {
        return new Meal(MealId.random(), name, order, time, notes, target, new ArrayList<>());
    }

    /**
     * Reconstrói a partir do estado persistido (uso exclusivo da infraestrutura).
     */
    public static Meal reconstitute(MealId id, String name, int order, String time,
                                    String notes, Macronutrients target,
                                    List<MealItem> items) {
        return new Meal(id, name, order, time, notes, target, items);
    }

    /**
     * Adiciona um item à refeição.
     */
    public void addItem(MealItem item) {
        Objects.requireNonNull(item, "item");
        items.add(item);
    }

    /**
     * Remove um item da refeição pelo id.
     */
    public boolean removeItem(MealItemId itemId) {
        return items.removeIf(item -> item.id().equals(itemId));
    }

    /**
     * Retorna os totais calculados a partir dos itens (snapshot macros).
     * Nunca digitado manualmente — sempre derivado.
     * Retorna null se a refeição não tem itens.
     */
    public Macronutrients calculateTotals() {
        if (items.isEmpty()) {
            return null;
        }
        double calories = 0;
        double protein = 0;
        double carbohydrates = 0;
        double fat = 0;
        for (MealItem item : items) {
            Macronutrients m = item.macrosSnapshot();
            calories += m.calories();
            protein += m.protein();
            carbohydrates += m.carbohydrates();
            fat += m.fat();
        }
        return Macronutrients.of(calories, protein, carbohydrates, fat);
    }

    public MealId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int order() {
        return order;
    }

    public String time() {
        return time;
    }

    public String notes() {
        return notes;
    }

    public Macronutrients target() {
        return target;
    }

    public List<MealItem> items() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Meal that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Meal[id=" + id + ", name=" + name + ", order=" + order
                + ", items=" + items.size() + "]";
    }
}