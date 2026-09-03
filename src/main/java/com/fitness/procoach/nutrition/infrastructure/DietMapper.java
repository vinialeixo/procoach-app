package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.nutrition.domain.AthleteId;
import com.fitness.procoach.nutrition.domain.CoachId;
import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietId;
import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietStatus;
import com.fitness.procoach.nutrition.domain.Macronutrients;

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
		return entity;
	}

	static Diet toDomain(DietJpaEntity entity) {
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
				entity.isDeleted());
	}
}
