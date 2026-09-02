package com.fitness.procoach.library.interfaces;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fitness.procoach.library.domain.Exercise;
import com.fitness.procoach.library.domain.Equipment;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.MuscleGroup;

/** DTO de saída de um exercício. */
public record ExerciseResponse(
		UUID id,
		String name,
		List<MuscleGroup> muscleGroups,
		Equipment equipment,
		String execution,
		List<String> commonErrors,
		List<String> progressions,
		List<String> regressions,
		List<String> contraindications,
		List<ExerciseReference> references,
		UUID createdBy,
		Instant createdAt,
		Instant updatedAt) {

	public static ExerciseResponse from(Exercise e) {
		return new ExerciseResponse(e.getId(), e.getName(), e.getMuscleGroups(), e.getEquipment(),
				e.getExecution(), e.getCommonErrors(), e.getProgressions(), e.getRegressions(),
				e.getContraindications(), e.getReferences(), e.getCreatedBy(), e.getCreatedAt(),
				e.getUpdatedAt());
	}
}
