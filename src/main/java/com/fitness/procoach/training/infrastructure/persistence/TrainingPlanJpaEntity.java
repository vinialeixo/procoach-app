package com.fitness.procoach.training.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "training_plan")
public class TrainingPlanJpaEntity {

	@Id
	private UUID id;

	@Column(name = "athlete_id", nullable = false)
	private UUID athleteId;

	@Column(name = "coach_id", nullable = false)
	private UUID coachId;

	@Column(name = "periodization_id", nullable = false)
	private UUID periodizationId;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private int version;

	@Column(name = "previous_version_id")
	private UUID previousVersionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TrainingStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected TrainingPlanJpaEntity() {
	}

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID id) {
		this.id = id;
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

	public UUID getPeriodizationId() {
		return this.periodizationId;
	}

	public void setPeriodizationId(UUID periodizationId) {
		this.periodizationId = periodizationId;
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

	public int getVersion() {
		return this.version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public UUID getPreviousVersionId() {
		return this.previousVersionId;
	}

	public void setPreviousVersionId(UUID previousVersionId) {
		this.previousVersionId = previousVersionId;
	}

	public TrainingStatus getStatus() {
		return this.status;
	}

	public void setStatus(TrainingStatus status) {
		this.status = status;
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
