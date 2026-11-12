package com.fitness.procoach.nutrition.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MealTest {

    private static final Macronutrients EGG_MACROS = Macronutrients.of(143, 13.0, 1.6, 9.5);
    private static final Macronutrients RICE_MACROS = Macronutrients.of(130, 2.7, 28.0, 0.3);

    @Test
    void create_shouldBuildMeal() {
        Meal meal = Meal.create("Café da manhã", 1, "07:00", "Pós-jejum", null);

        assertThat(meal.id()).isNotNull();
        assertThat(meal.name()).isEqualTo("Café da manhã");
        assertThat(meal.order()).isEqualTo(1);
        assertThat(meal.time()).isEqualTo("07:00");
        assertThat(meal.notes()).isEqualTo("Pós-jejum");
        assertThat(meal.target()).isNull();
        assertThat(meal.items()).isEmpty();
    }

    @Test
    void create_shouldRequireName() {
        assertThatThrownBy(() -> Meal.create(null, 1, "07:00", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_shouldRejectNegativeOrder() {
        assertThatThrownBy(() -> Meal.create("Refeição", -1, "07:00", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order must be non-negative");
    }

    @Test
    void create_shouldAllowTarget() {
        Macronutrients target = Macronutrients.of(500, 40, 60, 15);
        Meal meal = Meal.create("Almoço", 2, "12:00", null, target);

        assertThat(meal.target()).isEqualTo(target);
    }

    @Test
    void addItem_shouldAddItemToMeal() {
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        MealItem item = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);

        meal.addItem(item);

        assertThat(meal.items()).hasSize(1);
        assertThat(meal.items().get(0)).isEqualTo(item);
    }

    @Test
    void addItem_shouldRejectNull() {
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        assertThatThrownBy(() -> meal.addItem(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void removeItem_shouldRemoveItemById() {
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        MealItem item1 = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);
        MealItem item2 = MealItem.create("food2", "Arroz", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "1 xícara", "cozido", RICE_MACROS);
        meal.addItem(item1);
        meal.addItem(item2);

        boolean removed = meal.removeItem(item1.id());

        assertThat(removed).isTrue();
        assertThat(meal.items()).hasSize(1);
        assertThat(meal.items().get(0).id()).isEqualTo(item2.id());
    }

    @Test
    void removeItem_shouldReturnFalseForNonExistentId() {
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        MealItem item = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);
        meal.addItem(item);

        boolean removed = meal.removeItem(MealItemId.random());

        assertThat(removed).isFalse();
        assertThat(meal.items()).hasSize(1);
    }

    @Test
    void calculateTotals_shouldSumItemMacros() {
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        MealItem item1 = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);
        MealItem item2 = MealItem.create("food2", "Arroz", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "1 xícara", "cozido", RICE_MACROS);
        meal.addItem(item1);
        meal.addItem(item2);

        Macronutrients totals = meal.calculateTotals();

        assertThat(totals.calories()).isEqualTo(143 + 130);
        assertThat(totals.protein()).isEqualTo(13.0 + 2.7);
        assertThat(totals.carbohydrates()).isEqualTo(1.6 + 28.0);
        assertThat(totals.fat()).isEqualTo(9.5 + 0.3);
    }

    @Test
    void calculateTotals_shouldReturnNullForEmptyMeal() {
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);

        Macronutrients totals = meal.calculateTotals();

        assertThat(totals).isNull();
    }

    @Test
    void items_shouldReturnUnmodifiableList() {
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        MealItem item = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);
        meal.addItem(item);

        assertThatThrownBy(() -> meal.items().add(item))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reconstitute_shouldRestoreState() {
        MealId id = MealId.random();
        MealItem item = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);
        Macronutrients target = Macronutrients.of(500, 40, 60, 15);

        Meal meal = Meal.reconstitute(id, "Café da manhã", 1, "07:00", "Pós-jejum", target,
                java.util.List.of(item));

        assertThat(meal.id()).isEqualTo(id);
        assertThat(meal.name()).isEqualTo("Café da manhã");
        assertThat(meal.order()).isEqualTo(1);
        assertThat(meal.time()).isEqualTo("07:00");
        assertThat(meal.notes()).isEqualTo("Pós-jejum");
        assertThat(meal.target()).isEqualTo(target);
        assertThat(meal.items()).hasSize(1);
        assertThat(meal.items().get(0).id()).isEqualTo(item.id());
    }

    @Test
    void equals_and_hashCode_shouldWorkById() {
        MealId id = MealId.random();
        Meal meal1 = Meal.reconstitute(id, "Meal 1", 1, "07:00", null, null, java.util.List.of());
        Meal meal2 = Meal.reconstitute(id, "Meal 2", 2, "12:00", null, null, java.util.List.of());

        assertThat(meal1).isEqualTo(meal2);
        assertThat(meal1.hashCode()).isEqualTo(meal2.hashCode());
    }
}