package com.fitness.procoach.training.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class ExercisePrescription {

	private final UUID id;

	private final UUID exerciseId;

	private final String exerciseName;

	private final boolean customExercise;

	private final int orderIndex;

	private final int sets;

	private final int repetitions;

	private final BigDecimal load;

	private final IntensityType intensityType;

	private final BigDecimal intensityValue;

	private final int restSeconds;

	private final String notes;

	public ExercisePrescription(UUID id, UUID exerciseId, String exerciseName, boolean customExercise, int orderIndex,
			int sets, int repetitions, BigDecimal load, IntensityType intensityType, BigDecimal intensityValue,
			int restSeconds, String notes) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.exerciseId = exerciseId;
		this.exerciseName = requireText(exerciseName, "exerciseName");
		this.customExercise = customExercise;
		this.orderIndex = orderIndex;
		this.sets = requirePositive(sets, "sets");
		this.repetitions = requirePositive(repetitions, "repetitions");
		this.load = requirePositive(load, "load");
		this.intensityType = Objects.requireNonNull(intensityType, "intensityType must not be null");
		this.intensityValue = requirePositive(intensityValue, "intensityValue");
		this.restSeconds = requirePositive(restSeconds, "restSeconds");
		this.notes = notes;
	}

	public static ExercisePrescription create(UUID exerciseId, String exerciseName, boolean customExercise,
			int orderIndex, int sets, int repetitions, BigDecimal load, IntensityType intensityType,
			BigDecimal intensityValue, int restSeconds, String notes) {
		return new ExercisePrescription(UUID.randomUUID(), exerciseId, exerciseName, customExercise, orderIndex, sets,
				repetitions, load, intensityType, intensityValue, restSeconds, notes);
	}

	private static int requirePositive(int value, String field) {
		if (value <= 0) {
			throw new IllegalArgumentException(field + " must be a positive number, got: " + value);
		}
		return value;
	}

	private static BigDecimal requirePositive(BigDecimal value, String field) {
		Objects.requireNonNull(value, field + " must not be null");
		if (value.signum() <= 0) {
			throw new IllegalArgumentException(field + " must be a positive number, got: " + value);
		}
		return value;
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getExerciseId() {
		return this.exerciseId;
	}

	public String getExerciseName() {
		return this.exerciseName;
	}

	public boolean isCustomExercise() {
		return this.customExercise;
	}

	public int getOrderIndex() {
		return this.orderIndex;
	}

	public int getSets() {
		return this.sets;
	}

	public int getRepetitions() {
		return this.repetitions;
	}

	public BigDecimal getLoad() {
		return this.load;
	}

	public IntensityType getIntensityType() {
		return this.intensityType;
	}

	public BigDecimal getIntensityValue() {
		return this.intensityValue;
	}

	public int getRestSeconds() {
		return this.restSeconds;
	}

	public String getNotes() {
		return this.notes;
	}

}
