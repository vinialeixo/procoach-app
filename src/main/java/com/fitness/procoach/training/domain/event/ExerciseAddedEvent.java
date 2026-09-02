package com.fitness.procoach.training.domain.event;

import java.time.Instant;
import java.util.UUID;

public record ExerciseAddedEvent(UUID trainingId, UUID exercisePrescriptionId, UUID exerciseId, String exerciseName,
		boolean customExercise, int sets, int repetitions, Instant occurredAt) {

	public ExerciseAddedEvent(UUID trainingId, UUID exercisePrescriptionId, UUID exerciseId, String exerciseName,
			boolean customExercise, int sets, int repetitions) {
		this(trainingId, exercisePrescriptionId, exerciseId, exerciseName, customExercise, sets, repetitions,
				Instant.now());
	}

}
