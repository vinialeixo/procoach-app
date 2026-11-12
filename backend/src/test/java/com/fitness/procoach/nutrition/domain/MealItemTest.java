package com.fitness.procoach.nutrition.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MealItemTest {

    private static final Macronutrients MACROS = Macronutrients.of(143, 13.0, 1.6, 9.5);

    @Test
    void create_shouldBuildItemWithSnapshot() {
        MealItem item = MealItem.create(
                "taco:ovo-de-galinha-inteiro",
                "Ovo, de galinha, inteiro",
                FoodSource.TACO,
                FoodKind.FOOD,
                FoodState.RAW,
                100.0,
                "2 unidades",
                "cozido",
                MACROS);

        assertThat(item.id()).isNotNull();
        assertThat(item.foodId()).isEqualTo("taco:ovo-de-galinha-inteiro");
        assertThat(item.name()).isEqualTo("Ovo, de galinha, inteiro");
        assertThat(item.source()).isEqualTo(FoodSource.TACO);
        assertThat(item.kind()).isEqualTo(FoodKind.FOOD);
        assertThat(item.state()).isEqualTo(FoodState.RAW);
        assertThat(item.quantityG()).isEqualTo(100.0);
        assertThat(item.householdMeasure()).isEqualTo("2 unidades");
        assertThat(item.preparation()).isEqualTo("cozido");
        assertThat(item.macrosSnapshot()).isEqualTo(MACROS);
    }

    @Test
    void create_shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> MealItem.create(
                "food1", "Food", FoodSource.TACO, FoodKind.FOOD, FoodState.RAW,
                0, null, null, MACROS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantityG must be positive");

        assertThatThrownBy(() -> MealItem.create(
                "food1", "Food", FoodSource.TACO, FoodKind.FOOD, FoodState.RAW,
                -10, null, null, MACROS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantityG must be positive");
    }

    @Test
    void create_shouldRequireName() {
        assertThatThrownBy(() -> MealItem.create(
                "food1", null, FoodSource.TACO, FoodKind.FOOD, FoodState.RAW,
                100, null, null, MACROS))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_shouldRequireSource() {
        assertThatThrownBy(() -> MealItem.create(
                "food1", "Food", null, FoodKind.FOOD, FoodState.RAW,
                100, null, null, MACROS))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_shouldRequireKind() {
        assertThatThrownBy(() -> MealItem.create(
                "food1", "Food", FoodSource.TACO, null, FoodState.RAW,
                100, null, null, MACROS))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_shouldRequireState() {
        assertThatThrownBy(() -> MealItem.create(
                "food1", "Food", FoodSource.TACO, FoodKind.FOOD, null,
                100, null, null, MACROS))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_shouldRequireMacrosSnapshot() {
        assertThatThrownBy(() -> MealItem.create(
                "food1", "Food", FoodSource.TACO, FoodKind.FOOD, FoodState.RAW,
                100, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_shouldAllowOptionalFields() {
        MealItem item = MealItem.create(
                "custom:food1",
                "Custom Food",
                FoodSource.CUSTOM,
                FoodKind.FOOD,
                FoodState.AS_SERVED,
                50.0,
                null, // householdMeasure
                null, // preparation
                MACROS);

        assertThat(item.householdMeasure()).isNull();
        assertThat(item.preparation()).isNull();
        assertThat(item.source()).isEqualTo(FoodSource.CUSTOM);
        assertThat(item.state()).isEqualTo(FoodState.AS_SERVED);
    }

    @Test
    void create_shouldAllowSupplementCaloricKind() {
        MealItem item = MealItem.create(
                "custom:whey",
                "Whey Protein",
                FoodSource.CUSTOM,
                FoodKind.SUPPLEMENT_CALORIC,
                FoodState.AS_SERVED,
                30.0,
                "1 scoop",
                null,
                Macronutrients.of(120, 25, 2, 1));

        assertThat(item.kind()).isEqualTo(FoodKind.SUPPLEMENT_CALORIC);
    }

    @Test
    void reconstitute_shouldRestoreState() {
        MealItemId id = MealItemId.random();
        MealItem original = MealItem.reconstitute(
                id, "food1", "Food", FoodSource.USDA, FoodKind.FOOD, FoodState.RAW,
                100.0, "measure", "prep", MACROS);

        assertThat(original.id()).isEqualTo(id);
        assertThat(original.foodId()).isEqualTo("food1");
        assertThat(original.name()).isEqualTo("Food");
        assertThat(original.source()).isEqualTo(FoodSource.USDA);
        assertThat(original.kind()).isEqualTo(FoodKind.FOOD);
        assertThat(original.state()).isEqualTo(FoodState.RAW);
        assertThat(original.quantityG()).isEqualTo(100.0);
        assertThat(original.householdMeasure()).isEqualTo("measure");
        assertThat(original.preparation()).isEqualTo("prep");
        assertThat(original.macrosSnapshot()).isEqualTo(MACROS);
    }

    @Test
    void equals_and_hashCode_shouldWorkById() {
        MealItemId id = MealItemId.random();
        MealItem item1 = MealItem.reconstitute(id, "f1", "Food 1", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, null, null, MACROS);
        MealItem item2 = MealItem.reconstitute(id, "f2", "Food 2", FoodSource.USDA, FoodKind.FOOD,
                FoodState.RAW, 200, null, null, MACROS);

        assertThat(item1).isEqualTo(item2);
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
    }
}