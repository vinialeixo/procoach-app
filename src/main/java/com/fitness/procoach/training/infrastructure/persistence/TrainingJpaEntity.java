package com.fitness.procoach.training.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "training_training")
public class TrainingJpaEntity {

	@Id
	private UUID id;

	@Column(name = "periodization_id", nullable = false)
	private UUID periodizationId;

	@Column(name = "athlete_id", nullable = false)
	private UUID athleteId;

	@Column(name = "coach_id", nullable = false)
	private UUID coachId;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "week_number", nullable = false)
	private int weekNumber;

	@Column(name = "day_of_week", nullable = false)
	private int dayOfWeek;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TrainingStatus status;

	@OneToMany(mappedBy = "trainingId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("orderIndex ASC")
	private List<ExercisePrescriptionJpaEntity> exercises = new ArrayList<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected TrainingJpaEntity() {
	}

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getPeriodizationId() {
		return this.periodizationId;
	}

	public void setPeriodizationId(UUID periodizationId) {
		this.periodizationId = periodizationId;
	}

	public UUID getAthleteId() {
		return this.athleteId;
	}

	public void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	public UUID getCoachId() {
		return this.coachId;
	}

	public void setCoachId(UUID coachId) {
		this.coachId = coachId;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getWeekNumber() {
		return this.weekNumber;
	}

	public void setWeekNumber(int weekNumber) {
		this.weekNumber = weekNumber;
	}

	public int getDayOfWeek() {
		return this.dayOfWeek;
	}

	public void setDayOfWeek(int dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public TrainingStatus getStatus() {
		return this.status;
	}

	public void setStatus(TrainingStatus status) {
		this.status = status;
	}

	public List<ExercisePrescriptionJpaEntity> getExercises() {
		return this.exercises;
	}

	public void setExercises(List<ExercisePrescriptionJpaEntity> exercises) {
		this.exercises = exercises;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public boolean isDeleted() {
		return this.deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public Instant getDeletedAt() {
		return this.deletedAt;
	}

	public void setDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

}
