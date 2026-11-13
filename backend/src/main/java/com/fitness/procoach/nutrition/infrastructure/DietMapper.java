package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.nutrition.domain.AthleteId;
import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietId;
import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietStatus;
import com.fitness.procoach.nutrition.domain.DietWarning;
import com.fitness.procoach.nutrition.domain.FoodKind;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import com.fitness.procoach.nutrition.domain.Macronutrients;
import com.fitness.procoach.nutrition.domain.Meal;
import com.fitness.procoach.nutrition.domain.MealId;
import com.fitness.procoach.nutrition.domain.MealItem;
import com.fitness.procoach.nutrition.domain.MealItemId;
import com.fitness.procoach.nutrition.domain.Supplement;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapeamento entre o agregado de domínio e a entidade JPA.
 */
final class DietMapper {

    private DietMapper() {
    }

    static DietJpaEntity toEntity(Diet diet) {
        DietJpaEntity entity = new DietJpaEntity();
        entity.setId(diet.id().value());
        entity.setCoachId(diet.coachId().value());
        entity.setAthleteId(diet.athleteId().value());
        entity.setObjective(diet.objective());
        entity.setCalories(diet.macros().calories());
        entity.setProtein(diet.macros().protein());
        entity.setCarbohydrates(diet.macros().carbohydrates());
        entity.setFat(diet.macros().fat());
        entity.setStatus(diet.status());
        entity.setVersion(diet.version());
        entity.setPreviousVersionId(diet.previousVersionId() == null ? null : diet.previousVersionId().value());
        entity.setChangeReason(diet.changeReason());
        entity.setCreatedBy(diet.createdBy());
        entity.setUpdatedBy(diet.updatedBy());
        entity.setCreatedAt(diet.createdAt());
        entity.setUpdatedAt(diet.updatedAt());
        entity.setReadOnly(diet.isReadOnly());
        entity.setDeleted(diet.isDeleted());

        // Map meals and meal items
        if (diet.meals() != null && !diet.meals().isEmpty()) {
            for (Meal meal : diet.meals()) {
                MealJpaEntity mealEntity = toEntity(meal);
                mealEntity.setDiet(entity);
                entity.addMeal(mealEntity);
            }
        }

        // Map supplements (T-090)
        if (diet.supplements() != null && !diet.supplements().isEmpty()) {
            for (Supplement supplement : diet.supplements()) {
                SupplementJpaEntity supplementEntity = toEntity(supplement);
                supplementEntity.setDiet(entity);
                entity.addSupplement(supplementEntity);
            }
        }
        return entity;
    }

    private static SupplementJpaEntity toEntity(Supplement supplement) {
        SupplementJpaEntity entity = new SupplementJpaEntity();
        entity.setId(java.util.UUID.randomUUID());
        entity.setName(supplement.name());
        entity.setDose(supplement.dose());
        entity.setTiming(supplement.timing());
        entity.setCountsTowardMacros(supplement.countsTowardMacros());
        return entity;
    }

    private static MealJpaEntity toEntity(Meal meal) {
        MealJpaEntity mealEntity = new MealJpaEntity();
        mealEntity.setId(meal.id().value());
        mealEntity.setName(meal.name());
        mealEntity.setOrderIndex(meal.order());
        mealEntity.setTime(meal.time());
        mealEntity.setNotes(meal.notes());
        if (meal.target() != null) {
            mealEntity.setTargetCalories(meal.target().calories());
            mealEntity.setTargetProtein(meal.target().protein());
            mealEntity.setTargetCarbohydrates(meal.target().carbohydrates());
            mealEntity.setTargetFat(meal.target().fat());
        }
        if (meal.items() != null && !meal.items().isEmpty()) {
            for (MealItem item : meal.items()) {
                MealItemJpaEntity itemEntity = toEntity(item);
                itemEntity.setMeal(mealEntity);
                mealEntity.getItems().add(itemEntity);
            }
        }
        return mealEntity;
    }

    private static MealItemJpaEntity toEntity(MealItem item) {
        MealItemJpaEntity itemEntity = new MealItemJpaEntity();
        itemEntity.setId(item.id().value());
        itemEntity.setFoodId(item.foodId());
        itemEntity.setName(item.name());
        itemEntity.setSource(item.source());
        itemEntity.setKind(item.kind());
        itemEntity.setState(item.state());
        itemEntity.setQuantityG(item.quantityG());
        itemEntity.setHouseholdMeasure(item.householdMeasure());
        itemEntity.setPreparation(item.preparation());
        itemEntity.setCalories(item.macrosSnapshot().calories());
        itemEntity.setProtein(item.macrosSnapshot().protein());
        itemEntity.setCarbohydrates(item.macrosSnapshot().carbohydrates());
        itemEntity.setFat(item.macrosSnapshot().fat());
        return itemEntity;
    }

    static Diet toDomain(DietJpaEntity entity) {
        List<Meal> meals = new ArrayList<>();
        if (entity.getMeals() != null) {
            for (MealJpaEntity mealEntity : entity.getMeals()) {
                meals.add(toDomain(mealEntity));
            }
        }

        // Map supplements (T-090)
        List<Supplement> supplements = new ArrayList<>();
        if (entity.getSupplements() != null) {
            for (SupplementJpaEntity supplementEntity : entity.getSupplements()) {
                supplements.add(toDomain(supplementEntity));
            }
        }

        // Warnings are not persisted; they are generated on-the-fly based on current athlete profile
        List<DietWarning> warnings = List.of();
        return Diet.reconstitute(
                DietId.of(entity.getId()),
                CoachId.of(entity.getCoachId()),
                AthleteId.of(entity.getAthleteId()),
                DietObjective.valueOf(entity.getObjective().name()),
                Macronutrients.of(entity.getCalories(), entity.getProtein(),
                        entity.getCarbohydrates(), entity.getFat()),
                DietStatus.valueOf(entity.getStatus().name()),
                entity.getVersion(),
                entity.getPreviousVersionId() == null ? null : DietId.of(entity.getPreviousVersionId()),
                entity.getChangeReason(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isReadOnly(),
                entity.isDeleted(),
                meals,
                supplements,
                warnings);
    }

    private static Supplement toDomain(SupplementJpaEntity entity) {
        return Supplement.create(
                entity.getName(),
                entity.getDose(),
                entity.getTiming(),
                entity.isCountsTowardMacros());
    }

    private static Meal toDomain(MealJpaEntity mealEntity) {
        List<MealItem> items = new ArrayList<>();
        if (mealEntity.getItems() != null) {
            for (MealItemJpaEntity itemEntity : mealEntity.getItems()) {
                items.add(toDomain(itemEntity));
            }
        }
        Macronutrients target = null;
        if (mealEntity.getTargetCalories() != null) {
            target = Macronutrients.of(
                    mealEntity.getTargetCalories(),
                    mealEntity.getTargetProtein() != null ? mealEntity.getTargetProtein() : 0,
                    mealEntity.getTargetCarbohydrates() != null ? mealEntity.getTargetCarbohydrates() : 0,
                    mealEntity.getTargetFat() != null ? mealEntity.getTargetFat() : 0);
        }
        return Meal.reconstitute(
                MealId.of(mealEntity.getId()),
                mealEntity.getName(),
                mealEntity.getOrderIndex(),
                mealEntity.getTime(),
                mealEntity.getNotes(),
                target,
                items);
    }

    private static MealItem toDomain(MealItemJpaEntity itemEntity) {
        return MealItem.reconstitute(
                MealItemId.of(itemEntity.getId()),
                itemEntity.getFoodId(),
                itemEntity.getName(),
                itemEntity.getSource(),
                itemEntity.getKind(),
                itemEntity.getState(),
                itemEntity.getQuantityG(),
                itemEntity.getHouseholdMeasure(),
                itemEntity.getPreparation(),
                Macronutrients.of(itemEntity.getCalories(), itemEntity.getProtein(),
                        itemEntity.getCarbohydrates(), itemEntity.getFat()));
    }
}