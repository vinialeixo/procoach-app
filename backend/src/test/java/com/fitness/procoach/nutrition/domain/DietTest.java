package com.fitness.procoach.nutrition.domain;

import com.fitness.procoach.coach.domain.CoachId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DietTest {

    private static final CoachId COACH = CoachId.of(UUID.randomUUID());
    private static final AthleteId ATHLETE = AthleteId.of(UUID.randomUUID());
    private static final UUID AUTHOR = UUID.randomUUID();
    private static final Macronutrients MACROS = Macronutrients.of(2500, 180, 250, 80);

    @Test
    void create_shouldBuildFirstVersionAsDraft() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);

        assertThat(diet.version()).isEqualTo(1);
        assertThat(diet.status()).isEqualTo(DietStatus.DRAFT);
        assertThat(diet.objective()).isEqualTo(DietObjective.HYPERTROPHY);
        assertThat(diet.macros()).isEqualTo(MACROS);
        assertThat(diet.coachId()).isEqualTo(COACH);
        assertThat(diet.athleteId()).isEqualTo(ATHLETE);
        assertThat(diet.previousVersionId()).isNull();
        assertThat(diet.isReadOnly()).isFalse();
        assertThat(diet.isDeleted()).isFalse();
        assertThat(diet.createdBy()).isEqualTo(AUTHOR);
    }

    @Test
    void create_shouldEmitDietCreatedEvent() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        assertThat(diet.domainEvents()).hasSize(1);
        assertThat(diet.domainEvents().get(0)).isInstanceOf(DietCreated.class);
    }

    @Test
    void createNewVersion_shouldChainHistoryAndFreezePreviousVersion() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        Macronutrients newMacros = Macronutrients.of(2200, 200, 200, 60);
        Diet v2 = v1.createNewVersion(DietObjective.CUTTING, newMacros, "Redução de calorias", AUTHOR);

        assertThat(v2.version()).isEqualTo(2);
        assertThat(v2.previousVersionId()).isEqualTo(v1.id());
        assertThat(v2.macros()).isEqualTo(newMacros);
        assertThat(v2.status()).isEqualTo(DietStatus.DRAFT);
        assertThat(v2.changeReason()).isEqualTo("Redução de calorias");
        assertThat(v1.isReadOnly()).isTrue();
        assertThat(v1.domainEvents()).hasSize(1);
        assertThat(v1.domainEvents().get(0)).isInstanceOf(DietCreated.class);
        assertThat(v2.domainEvents()).hasSize(1);
        assertThat(v2.domainEvents().get(0)).isInstanceOf(DietVersioned.class);
    }

    @Test
    void createNewVersion_shouldRequireChangeReason() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        assertThatThrownBy(() -> v1.createNewVersion(DietObjective.CUTTING, MACROS, "  ", AUTHOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("changeReason");
    }

    @Test
    void createNewVersion_shouldRejectFromReadOnlyVersion() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);
        v1.createNewVersion(DietObjective.CUTTING, MACROS, "Ajuste", AUTHOR);

        assertThatThrownBy(() -> v1.createNewVersion(DietObjective.CUTTING, MACROS, "Outro", AUTHOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void createNewVersion_shouldRejectFromDeletedDiet() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);
        v1.softDelete(AUTHOR);

        assertThatThrownBy(() -> v1.createNewVersion(DietObjective.CUTTING, MACROS, "Ajuste", AUTHOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deleted");
    }

    @Test
    void changeStatus_shouldFollowApprovalFlow() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.changeStatus(DietStatus.IN_REVIEW);
        assertThat(diet.status()).isEqualTo(DietStatus.IN_REVIEW);

        diet.changeStatus(DietStatus.APPROVED);
        assertThat(diet.status()).isEqualTo(DietStatus.APPROVED);

        diet.changeStatus(DietStatus.ARCHIVED);
        assertThat(diet.status()).isEqualTo(DietStatus.ARCHIVED);
    }

    @Test
    void changeStatus_shouldEmitDietStatusChangedEvent() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.changeStatus(DietStatus.IN_REVIEW);

        assertThat(diet.domainEvents()).hasSize(2);
        assertThat(diet.domainEvents().get(1)).isInstanceOf(DietStatusChanged.class);
    }

    @Test
    void changeStatus_shouldBlockInvalidTransitions() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        assertThatThrownBy(() -> diet.changeStatus(DietStatus.APPROVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void changeStatus_shouldRejectOnReadOnlyVersion() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);
        v1.createNewVersion(DietObjective.CUTTING, MACROS, "Ajuste", AUTHOR);

        assertThatThrownBy(() -> v1.changeStatus(DietStatus.IN_REVIEW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void softDelete_shouldMarkAsDeletedAndEmitEvent() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.softDelete(AUTHOR);

        assertThat(diet.isDeleted()).isTrue();
        assertThat(diet.domainEvents()).hasSize(2);
        assertThat(diet.domainEvents().get(1)).isInstanceOf(DietDeleted.class);
    }

    @Test
    void softDelete_shouldBeIdempotent() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.softDelete(AUTHOR);
        diet.softDelete(AUTHOR);

        assertThat(diet.isDeleted()).isTrue();
        assertThat(diet.domainEvents()).hasSize(2);
    }

    @Test
    void macronutrients_shouldRejectInvalidValues() {
        // calories must be > 0
        assertThatThrownBy(() -> Macronutrients.of(0, 180, 250, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("calories");
        assertThatThrownBy(() -> Macronutrients.of(-100, 180, 250, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("calories");
        // protein must be > 0
        assertThatThrownBy(() -> Macronutrients.of(2500, 0, 250, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protein");
        assertThatThrownBy(() -> Macronutrients.of(2500, -1, 250, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protein");
        // carbohydrates must be >= 0 (zero allowed for ketogenic/peak week)
        assertThatThrownBy(() -> Macronutrients.of(2500, 180, -1, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("carbohydrates");
        // fat must be >= 0 (zero allowed for specific strategies)
        assertThatThrownBy(() -> Macronutrients.of(2500, 180, 250, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fat");
    }

    @Test
    void macronutrients_shouldAllowZeroCarbohydratesAndFat() {
        // Zero carbs (ketogenic)
        Macronutrients keto = Macronutrients.of(2000, 150, 0, 150);
        assertThat(keto.carbohydrates()).isZero();
        // Zero fat (specific strategy)
        Macronutrients lowFat = Macronutrients.of(2000, 150, 200, 0);
        assertThat(lowFat.fat()).isZero();
    }

    @Test
    void clearDomainEvents_shouldEmptyTheList() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.clearDomainEvents();

        assertThat(diet.domainEvents()).isEmpty();
    }

    // ===== T-085: Meal Tests =====

    private static final Macronutrients EGG_MACROS = Macronutrients.of(143, 13.0, 1.6, 9.5);
    private static final Macronutrients RICE_MACROS = Macronutrients.of(130, 2.7, 28.0, 0.3);
    private static final Macronutrients CHICKEN_MACROS = Macronutrients.of(165, 31.0, 0.1, 3.6);

    @Test
    void meals_shouldInitiallyBeEmpty() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);

        assertThat(diet.meals()).isEmpty();
    }

    @Test
    void addMeal_shouldAddMealToDiet() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);
        Meal meal = Meal.create("Café da manhã", 1, "07:00", "Pós-jejum", null);

        diet.addMeal(meal);

        assertThat(diet.meals()).hasSize(1);
        assertThat(diet.meals().get(0)).isEqualTo(meal);
    }

    @Test
    void addMeal_shouldRecalculateMacrosFromMeals() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        MealItem item = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);
        meal.addItem(item);

        diet.addMeal(meal);

        // Macros should be recalculated from meal items
        Macronutrients totals = diet.calculateTotalsFromMeals();
        assertThat(totals.calories()).isEqualTo(143);
        assertThat(totals.protein()).isEqualTo(13.0);
        assertThat(totals.carbohydrates()).isEqualTo(1.6);
        assertThat(totals.fat()).isEqualTo(9.5);
    }

    @Test
    void addMeal_shouldRejectOnReadOnlyVersion() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);
        v1.createNewVersion(DietObjective.CUTTING, MACROS, "Ajuste", AUTHOR);
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);

        assertThatThrownBy(() -> v1.addMeal(meal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void addMeal_shouldRejectOnDeletedDiet() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);
        diet.softDelete(AUTHOR);
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);

        assertThatThrownBy(() -> diet.addMeal(meal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deleted");
    }

    @Test
    void removeMeal_shouldRemoveMealAndRecalculate() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);
        Meal meal1 = Meal.create("Café da manhã", 1, "07:00", null, null);
        MealItem item1 = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);
        meal1.addItem(item1);
        Meal meal2 = Meal.create("Almoço", 2, "12:00", null, null);
        MealItem item2 = MealItem.create("food2", "Frango", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "1 filé", "grelhado", CHICKEN_MACROS);
        meal2.addItem(item2);
        diet.addMeal(meal1);
        diet.addMeal(meal2);

        boolean removed = diet.removeMeal(meal1.id());

        assertThat(removed).isTrue();
        assertThat(diet.meals()).hasSize(1);
        assertThat(diet.meals().get(0).id()).isEqualTo(meal2.id());

        // Macros should be recalculated
        Macronutrients totals = diet.calculateTotalsFromMeals();
        assertThat(totals.calories()).isEqualTo(165);
        assertThat(totals.protein()).isEqualTo(31.0);
    }

    @Test
    void removeMeal_shouldReturnFalseForNonExistentMeal() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        diet.addMeal(meal);

        boolean removed = diet.removeMeal(MealId.random());

        assertThat(removed).isFalse();
        assertThat(diet.meals()).hasSize(1);
    }

    @Test
    void removeMeal_shouldRejectOnReadOnlyVersion() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        v1.addMeal(meal);
        v1.createNewVersion(DietObjective.CUTTING, MACROS, "Ajuste", AUTHOR);

        assertThatThrownBy(() -> v1.removeMeal(meal.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void createNewVersion_shouldCopyMealsAndItems() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);
        Meal meal = Meal.create("Café da manhã", 1, "07:00", "Pós-jejum", null);
        MealItem item = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);
        meal.addItem(item);
        v1.addMeal(meal);

        Macronutrients newMacros = Macronutrients.of(2600, 200, 250, 85);
        Diet v2 = v1.createNewVersion(DietObjective.HYPERTROPHY, newMacros, "Ajuste de macros", AUTHOR);

        // v1 should be read-only
        assertThat(v1.isReadOnly()).isTrue();

        // v2 should have copied meals with new IDs
        assertThat(v2.meals()).hasSize(1);
        Meal copiedMeal = v2.meals().get(0);
        assertThat(copiedMeal.name()).isEqualTo("Café da manhã");
        assertThat(copiedMeal.order()).isEqualTo(1);
        assertThat(copiedMeal.time()).isEqualTo("07:00");
        assertThat(copiedMeal.notes()).isEqualTo("Pós-jejum");
        assertThat(copiedMeal.id()).isNotEqualTo(meal.id()); // new ID

        // Items should be copied with new IDs and snapshot macros
        assertThat(copiedMeal.items()).hasSize(1);
        MealItem copiedItem = copiedMeal.items().get(0);
        assertThat(copiedItem.name()).isEqualTo("Ovo");
        assertThat(copiedItem.macrosSnapshot()).isEqualTo(EGG_MACROS);
        assertThat(copiedItem.id()).isNotEqualTo(item.id()); // new ID

        // v2 macros should be recalculated from copied items
        Macronutrients v2Totals = v2.calculateTotalsFromMeals();
        assertThat(v2Totals.calories()).isEqualTo(143);
        assertThat(v2Totals.protein()).isEqualTo(13.0);

        // v2 should emit DietVersioned event
        assertThat(v2.domainEvents()).hasSize(1);
        assertThat(v2.domainEvents().get(0)).isInstanceOf(DietVersioned.class);
    }

    @Test
    void recalculateMacrosFromMeals_shouldUpdateMacrosWhenItemsChange() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        MealItem item = MealItem.create("food1", "Ovo", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "2 un", "cozido", EGG_MACROS);
        meal.addItem(item);
        diet.addMeal(meal);

        // Add another item
        MealItem item2 = MealItem.create("food2", "Arroz", FoodSource.TACO, FoodKind.FOOD,
                FoodState.RAW, 100, "1 xícara", "cozido", RICE_MACROS);
        meal.addItem(item2);
        diet.recalculateMacrosFromMeals();

        Macronutrients totals = diet.calculateTotalsFromMeals();
        assertThat(totals.calories()).isEqualTo(143 + 130);
        assertThat(totals.protein()).isEqualTo(13.0 + 2.7);
        assertThat(totals.carbohydrates()).isEqualTo(1.6 + 28.0);
        assertThat(totals.fat()).isEqualTo(9.5 + 0.3);
    }

    @Test
    void calculateTotalsFromMeals_shouldReturnStoredMacrosWhenNoMeals() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);

        Macronutrients totals = diet.calculateTotalsFromMeals();

        assertThat(totals).isEqualTo(MACROS);
    }

    @Test
    void meals_shouldReturnUnmodifiableList() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);
        Meal meal = Meal.create("Café da manhã", 1, "07:00", null, null);
        diet.addMeal(meal);

        assertThatThrownBy(() -> diet.meals().add(meal))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
