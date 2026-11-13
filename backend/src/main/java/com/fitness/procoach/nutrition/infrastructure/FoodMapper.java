package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.nutrition.domain.Food;
import com.fitness.procoach.nutrition.domain.FoodId;
import com.fitness.procoach.nutrition.domain.FoodMacros;

/**
 * Mapeamento entre o agregado Food e a entidade JPA.
 */
final class FoodMapper {

	private FoodMapper() {
	}

	static FoodJpaEntity toEntity(Food food) {
		FoodJpaEntity entity = new FoodJpaEntity();
		entity.setId(food.id().value());
		entity.setTacoId(food.tacoId());
		entity.setName(food.name());
		entity.setCategory(food.category());
		entity.setCalories(food.macrosPer100g().calories());
		entity.setProtein(food.macrosPer100g().protein());
		entity.setCarbohydrates(food.macrosPer100g().carbohydrates());
		entity.setFat(food.macrosPer100g().fat());
		entity.setEdiblePortionPercent(food.ediblePortionPercent());
		entity.setHouseholdMeasure(food.householdMeasure());
		entity.setHouseholdMeasureGrams(food.householdMeasureGrams());
		entity.setSource(food.source());
		entity.setStateDefault(food.stateDefault());
		entity.setCoachId(food.coachId());
		entity.setCoachApproved(food.coachApproved());
		entity.setDeleted(food.isDeleted());
		entity.setCreatedAt(food.createdAt());
		entity.setUpdatedAt(food.updatedAt());
		return entity;
	}

	static Food toDomain(FoodJpaEntity entity) {
		return Food.reconstitute(
				FoodId.of(entity.getId()),
				entity.getTacoId(),
				entity.getName(),
				entity.getCategory(),
				FoodMacros.of(entity.getCalories(), entity.getProtein(),
						entity.getCarbohydrates(), entity.getFat()),
				entity.getEdiblePortionPercent(),
				entity.getHouseholdMeasure(),
				entity.getHouseholdMeasureGrams(),
				entity.getSource(),
				entity.getStateDefault(),
				entity.getCoachId(),
				entity.isCoachApproved(),
				entity.isDeleted(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
