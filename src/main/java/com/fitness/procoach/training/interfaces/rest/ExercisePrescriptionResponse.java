package com.fitness.procoach.training.interfaces.rest;

import java.math.BigDecimal;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.ExercisePrescription;
import com.fitness.procoach.training.domain.model.IntensityType;

public record ExercisePrescriptionResponse(UUID id, UUID exerciseId, String exerciseName, boolean customExercise,
		int orderIndex, int sets, int repetitions, BigDecimal load, IntensityType intensityType,
		BigDecimal intensityValue, int restSeconds, String notes) {

	public static ExercisePrescriptionResponse from(ExercisePrescription e) {
		return new ExercisePrescriptionResponse(e.getId(), e.getExerciseId(), e.getExerciseName(), e.isCustomExercise(),
				e.getOrderIndex(), e.getSets(), e.getRepetitions(), e.getLoad(), e.getIntensityType(),
				e.getIntensityValue(), e.getRestSeconds(), e.getNotes());
	}

}
