package com.fitness.procoach.training.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.IntensityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "training_exercise_prescription")
public class ExercisePrescriptionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "training_id", nullable = false)
	private UUID trainingId;

	@Column(name = "exercise_id")
	private UUID exerciseId;

	@Column(name = "exercise_name", nullable = false)
	private String exerciseName;

	@Column(name = "custom_exercise", nullable = false)
	private boolean customExercise;

	@Column(name = "order_index", nullable = false)
	private int orderIndex;

	@Column(nullable = false)
	private int sets;

	@Column(nullable = false)
	private int repetitions;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal load;

	@Enumerated(EnumType.STRING)
	@Column(name = "intensity_type", nullable = false)
	private IntensityType intensityType;

	@Column(name = "intensity_value", nullable = false, precision = 10, scale = 2)
	private BigDecimal intensityValue;

	@Column(name = "rest_seconds", nullable = false)
	private int restSeconds;

	@Column(columnDefinition = "TEXT")
	private String notes;

	protected ExercisePrescriptionJpaEntity() {
	}

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getTrainingId() {
		return this.trainingId;
	}

	public void setTrainingId(UUID trainingId) {
		this.trainingId = trainingId;
	}

	public UUID getExerciseId() {
		return this.exerciseId;
	}

	public void setExerciseId(UUID exerciseId) {
		this.exerciseId = exerciseId;
	}

	public String getExerciseName() {
		return this.exerciseName;
	}

	public void setExerciseName(String exerciseName) {
		this.exerciseName = exerciseName;
	}

	public boolean isCustomExercise() {
		return this.customExercise;
	}

	public void setCustomExercise(boolean customExercise) {
		this.customExercise = customExercise;
	}

	public int getOrderIndex() {
		return this.orderIndex;
	}

	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	public int getSets() {
		return this.sets;
	}

	public void setSets(int sets) {
		this.sets = sets;
	}

	public int getRepetitions() {
		return this.repetitions;
	}

	public void setRepetitions(int repetitions) {
		this.repetitions = repetitions;
	}

	public BigDecimal getLoad() {
		return this.load;
	}

	public void setLoad(BigDecimal load) {
		this.load = load;
	}

	public IntensityType getIntensityType() {
		return this.intensityType;
	}

	public void setIntensityType(IntensityType intensityType) {
		this.intensityType = intensityType;
	}

	public BigDecimal getIntensityValue() {
		return this.intensityValue;
	}

	public void setIntensityValue(BigDecimal intensityValue) {
		this.intensityValue = intensityValue;
	}

	public int getRestSeconds() {
		return this.restSeconds;
	}

	public void setRestSeconds(int restSeconds) {
		this.restSeconds = restSeconds;
	}

	public String getNotes() {
		return this.notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

}
