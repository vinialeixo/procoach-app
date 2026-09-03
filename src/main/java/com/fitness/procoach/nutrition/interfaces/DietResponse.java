package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de saída de uma dieta.
 */
public record DietResponse(
		UUID id,
		UUID coachId,
		UUID athleteId,
		DietObjective objective,
		double calories,
		double protein,
		double carbohydrates,
		double fat,
		DietStatus status,
		int version,
		UUID previousVersionId,
		String changeReason,
		Instant createdAt,
		Instant updatedAt,
		boolean readOnly,
		boolean deleted) {

	public static DietResponse from(Diet diet) {
		return new DietResponse(diet.id().value(), diet.coachId().value(), diet.athleteId().value(),
				diet.objective(), diet.macros().calories(), diet.macros().protein(),
				diet.macros().carbohydrates(), diet.macros().fat(), diet.status(), diet.version(),
				diet.previousVersionId() == null ? null : diet.previousVersionId().value(),
				diet.changeReason(), diet.createdAt(), diet.updatedAt(), diet.isReadOnly(),
				diet.isDeleted());
	}
}
