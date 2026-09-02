package com.fitness.procoach.training.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Periodization {

	private final UUID id;

	private final UUID athleteId;

	private final UUID coachId;

	private String name;

	private String description;

	private final LocalDate startDate;

	private final LocalDate endDate;

	private PeriodizationStatus status;

	private final Instant createdAt;

	private Instant updatedAt;

	private boolean deleted;

	private Instant deletedAt;

	public Periodization(UUID id, UUID athleteId, UUID coachId, String name, String description, LocalDate startDate,
			LocalDate endDate, PeriodizationStatus status, Instant createdAt, Instant updatedAt, boolean deleted,
			Instant deletedAt) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.coachId = Objects.requireNonNull(coachId, "coachId must not be null");
		this.name = requireText(name, "name");
		this.description = description;
		this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
		this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
		if (endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("endDate must not be before startDate");
		}
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		this.deleted = deleted;
		this.deletedAt = deletedAt;
	}

	public static Periodization create(UUID athleteId, UUID coachId, String name, String description,
			LocalDate startDate, LocalDate endDate) {
		Instant now = Instant.now();
		return new Periodization(UUID.randomUUID(), athleteId, coachId, name, description, startDate, endDate,
				PeriodizationStatus.DRAFT, now, now, false, null);
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}

	public void updateDetails(String name, String description) {
		ensureNotDeleted();
		this.name = requireText(name, "name");
		this.description = description;
		touch();
	}

	public void activate() {
		ensureNotDeleted();
		if (this.status != PeriodizationStatus.DRAFT) {
			throw new IllegalStateException("Only DRAFT periodizations can be activated, current: " + this.status);
		}
		this.status = PeriodizationStatus.ACTIVE;
		touch();
	}

	public void finish() {
		ensureNotDeleted();
		if (this.status != PeriodizationStatus.ACTIVE) {
			throw new IllegalStateException("Only ACTIVE periodizations can be finished, current: " + this.status);
		}
		this.status = PeriodizationStatus.FINISHED;
		touch();
	}

	public void archive() {
		ensureNotDeleted();
		if (this.status == PeriodizationStatus.ARCHIVED) {
			throw new IllegalStateException("Periodization is already archived");
		}
		this.status = PeriodizationStatus.ARCHIVED;
		touch();
	}

	public void softDelete() {
		ensureNotDeleted();
		this.deleted = true;
		this.deletedAt = Instant.now();
		touch();
	}

	private void ensureNotDeleted() {
		if (this.deleted) {
			throw new IllegalStateException("Periodization has been soft-deleted: " + this.id);
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

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public LocalDate getStartDate() {
		return this.startDate;
	}

	public LocalDate getEndDate() {
		return this.endDate;
	}

	public PeriodizationStatus getStatus() {
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
