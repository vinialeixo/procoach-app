package com.fitness.procoach.library.interfaces;

import java.util.List;
import java.util.UUID;

import com.fitness.procoach.library.domain.Equipment;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.MuscleGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** DTO de entrada para criação de exercício. */
public record CreateExerciseRequest(
		@NotBlank String name,
		@NotEmpty List<MuscleGroup> muscleGroups,
		@NotNull Equipment equipment,
		@NotBlank String execution,
		List<String> commonErrors,
		List<String> progressions,
		List<String> regressions,
		List<String> contraindications,
		List<ExerciseReference> references,
		@NotNull UUID createdBy) {
}
