package com.fitness.procoach.training.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TrainingPlan {

	private final UUID id;

	private final UUID athleteId;

	private final UUID coachId;

	private final UUID periodizationId;

	private String name;

	private String description;

	private final int version;

	private final UUID previousVersionId;

	private TrainingStatus status;

	private final Instant createdAt;

	private Instant updatedAt;

	private boolean deleted;

	private Instant deletedAt;

	public TrainingPlan(UUID id, UUID athleteId, UUID coachId, UUID periodizationId, String name, String description,
			int version, UUID previousVersionId, TrainingStatus status, Instant createdAt, Instant updatedAt,
			boolean deleted, Instant deletedAt) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.coachId = Objects.requireNonNull(coachId, "coachId must not be null");
		this.periodizationId = Objects.requireNonNull(periodizationId,
				"periodizationId must not be null — training plan must be linked to a periodization");
		this.name = requireText(name, "name");
		this.description = description;
		if (version < 1) {
			throw new IllegalArgumentException("version must be >= 1, got: " + version);
		}
		this.version = version;
		this.previousVersionId = previousVersionId;
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		this.deleted = deleted;
		this.deletedAt = deletedAt;
	}

	public static TrainingPlan create(UUID athleteId, UUID coachId, UUID periodizationId, String name,
			String description) {
		Instant now = Instant.now();
		return new TrainingPlan(UUID.randomUUID(), athleteId, coachId, periodizationId, name, description, 1, null,
				TrainingStatus.DRAFT, now, now, false, null);
	}

	public TrainingPlan createNewVersion(String name, String description) {
		ensureNotDeleted();
		Instant now = Instant.now();
		return new TrainingPlan(UUID.randomUUID(), this.athleteId, this.coachId, this.periodizationId, name, description,
				this.version + 1, this.id, TrainingStatus.DRAFT, now, now, false, null);
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}

	public void updateDetails(String name, String description) {
		ensureEditable();
		this.name = requireText(name, "name");
		this.description = description;
		touch();
	}

	public void transitionTo(TrainingStatus target) {
		ensureNotDeleted();
		if (!this.status.canTransitionTo(target)) {
			throw new IllegalStateException(
					"Invalid status transition: " + this.status + " -> " + target);
		}
		this.status = target;
		touch();
	}

	public void softDelete() {
		ensureNotDeleted();
		this.deleted = true;
		this.deletedAt = Instant.now();
		touch();
	}

	private void ensureEditable() {
		ensureNotDeleted();
		if (this.status != TrainingStatus.DRAFT && this.status != TrainingStatus.IN_REVIEW) {
			throw new IllegalStateException("TrainingPlan can only be edited in DRAFT or IN_REVIEW status, current: "
					+ this.status);
		}
	}

	private void ensureNotDeleted() {
		if (this.deleted) {
			throw new IllegalStateException("TrainingPlan has been soft-deleted: " + this.id);
		}
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getAthleteId() {
		return this.athleteId;
	}

	public UUID getCoachId() {
		return this.coachId;
	}

	public UUID getPeriodizationId() {
		return this.periodizationId;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public int getVersion() {
		return this.version;
	}

	public UUID getPreviousVersionId() {
		return this.previousVersionId;
	}

	public TrainingStatus getStatus() {
		return this.status;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

	public boolean isDeleted() {
		return this.deleted;
	}

	public Instant getDeletedAt() {
		return this.deletedAt;
	}

}
