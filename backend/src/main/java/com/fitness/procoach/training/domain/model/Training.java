package com.fitness.procoach.training.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Training {

	private final UUID id;

	private final UUID periodizationId;
	private final UUID planId;

	private final UUID athleteId;

	private final UUID coachId;

	private String name;

	private String description;

	private int weekNumber;

	private int dayOfWeek;

	private TrainingStatus status;

	private final List<ExercisePrescription> exercises;

	private final Instant createdAt;

	private Instant updatedAt;

	private boolean deleted;

	private Instant deletedAt;

	public Training(UUID id, UUID periodizationId, UUID planId, UUID athleteId, UUID coachId, String name,
			String description,
			int weekNumber, int dayOfWeek, TrainingStatus status, List<ExercisePrescription> exercises,
			Instant createdAt, Instant updatedAt, boolean deleted, Instant deletedAt) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.periodizationId = Objects.requireNonNull(periodizationId,
				"periodizationId must not be null — training must be linked to a periodization");
		// planId é nulo só em sessões antigas, de antes do vínculo com a versão do plano (V805).
		this.planId = planId;
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.coachId = Objects.requireNonNull(coachId, "coachId must not be null");
		this.name = requireText(name, "name");
		this.description = description;
		this.weekNumber = weekNumber;
		this.dayOfWeek = dayOfWeek;
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.exercises = new ArrayList<>(exercises != null ? exercises : List.of());
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		this.deleted = deleted;
		this.deletedAt = deletedAt;
	}

	public static Training create(UUID periodizationId, UUID planId, UUID athleteId, UUID coachId, String name,
			String description, int weekNumber, int dayOfWeek) {
		Objects.requireNonNull(planId, "planId must not be null — training must belong to a plan version");
		Instant now = Instant.now();
		return new Training(UUID.randomUUID(), periodizationId, planId, athleteId, coachId, name, description, weekNumber,
				dayOfWeek, TrainingStatus.DRAFT, List.of(), now, now, false, null);
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}

	public void addExercise(ExercisePrescription prescription) {
		ensureEditable();
		this.exercises.add(Objects.requireNonNull(prescription, "prescription must not be null"));
		sortExercises();
		touch();
	}

	/**
	 * Edita uma prescrição existente (séries, repetições, carga, intensidade, descanso, nome e
	 * observações), preservando id e posição. Em exercício da biblioteca, {@code exerciseId} pode
	 * apontar para outra versão do exercício ({@code null} mantém o atual); em exercício
	 * personalizado o id é sempre mantido.
	 */
	public void updateExercise(UUID exercisePrescriptionId, UUID exerciseId, String exerciseName, int sets,
			int repetitions, java.math.BigDecimal load, IntensityType intensityType,
			java.math.BigDecimal intensityValue, int restSeconds, String notes) {
		ensureEditable();
		for (int i = 0; i < this.exercises.size(); i++) {
			ExercisePrescription current = this.exercises.get(i);
			if (!current.getId().equals(exercisePrescriptionId)) {
				continue;
			}
			UUID newExerciseId = current.isCustomExercise() || exerciseId == null ? current.getExerciseId()
					: exerciseId;
			this.exercises.set(i,
					new ExercisePrescription(current.getId(), newExerciseId, exerciseName, current.isCustomExercise(),
							current.getOrderIndex(), sets, repetitions, load, intensityType, intensityValue,
							restSeconds, notes));
			touch();
			return;
		}
		throw new IllegalArgumentException("Exercise prescription not found: " + exercisePrescriptionId);
	}

	public void removeExercise(UUID exercisePrescriptionId) {
		ensureEditable();
		boolean removed = this.exercises.removeIf(e -> e.getId().equals(exercisePrescriptionId));
		if (!removed) {
			throw new IllegalArgumentException("Exercise prescription not found: " + exercisePrescriptionId);
		}
		sortExercises();
		touch();
	}

	public void reorderExercises(List<UUID> orderedPrescriptionIds) {
		ensureEditable();
		if (orderedPrescriptionIds == null || orderedPrescriptionIds.size() != this.exercises.size()) {
			throw new IllegalArgumentException("Ordered list must contain exactly the same prescriptions");
		}
		List<ExercisePrescription> reordered = new ArrayList<>();
		for (int i = 0; i < orderedPrescriptionIds.size(); i++) {
			UUID pid = orderedPrescriptionIds.get(i);
			ExercisePrescription found = this.exercises.stream()
				.filter(e -> e.getId().equals(pid))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + pid));
			reordered.add(new ExercisePrescription(found.getId(), found.getExerciseId(), found.getExerciseName(),
					found.isCustomExercise(), i, found.getSets(), found.getRepetitions(), found.getLoad(),
					found.getIntensityType(), found.getIntensityValue(), found.getRestSeconds(), found.getNotes()));
		}
		this.exercises.clear();
		this.exercises.addAll(reordered);
		touch();
	}

	/**
	 * Cópia desta sessão para uma nova versão do plano (T-067): mesmos dados e prescrições (com ids
	 * novos), em rascunho. A sessão original não muda.
	 */
	public Training copyForNewPlanVersion(UUID newPlanId) {
		Objects.requireNonNull(newPlanId, "newPlanId must not be null");
		ensureNotDeleted();
		Instant now = Instant.now();
		List<ExercisePrescription> copies = this.exercises.stream()
			.map(e -> new ExercisePrescription(UUID.randomUUID(), e.getExerciseId(), e.getExerciseName(),
					e.isCustomExercise(), e.getOrderIndex(), e.getSets(), e.getRepetitions(), e.getLoad(),
					e.getIntensityType(), e.getIntensityValue(), e.getRestSeconds(), e.getNotes()))
			.toList();
		return new Training(UUID.randomUUID(), this.periodizationId, newPlanId, this.athleteId, this.coachId,
				this.name, this.description, this.weekNumber, this.dayOfWeek, TrainingStatus.DRAFT, copies, now, now,
				false, null);
	}

	/**
	 * Publica a sessão junto com o plano: rascunho e revisão viram ACTIVE. Devolve {@code true}
	 * se a sessão foi publicada; sessão já ativa, finalizada ou removida fica como está.
	 */
	public boolean publish() {
		if (this.deleted) {
			return false;
		}
		if (this.status == TrainingStatus.DRAFT) {
			transitionTo(TrainingStatus.IN_REVIEW);
		}
		if (this.status == TrainingStatus.IN_REVIEW) {
			transitionTo(TrainingStatus.ACTIVE);
			return true;
		}
		return false;
	}

	/** Encerra a sessão quando o plano a que ela pertence é substituído; só atua em sessão ACTIVE. */
	public boolean finishIfActive() {
		if (!this.deleted && this.status == TrainingStatus.ACTIVE) {
			transitionTo(TrainingStatus.FINISHED);
			return true;
		}
		return false;
	}

	public void updateDetails(String name, String description, int weekNumber, int dayOfWeek) {
		ensureEditable();
		this.name = requireText(name, "name");
		this.description = description;
		this.weekNumber = weekNumber;
		this.dayOfWeek = dayOfWeek;
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
		// ACTIVE também é editável: o coach ajusta o treino em andamento e o atleta é avisado.
		if (this.status != TrainingStatus.DRAFT && this.status != TrainingStatus.IN_REVIEW
				&& this.status != TrainingStatus.ACTIVE) {
			throw new IllegalStateException(
					"Training can only be edited in DRAFT, IN_REVIEW or ACTIVE status, current: " + this.status);
		}
	}

	private void ensureNotDeleted() {
		if (this.deleted) {
			throw new IllegalStateException("Training has been soft-deleted: " + this.id);
		}
	}

	private void sortExercises() {
		this.exercises.sort(Comparator.comparingInt(ExercisePrescription::getOrderIndex));
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return this.id;
	}

	/** Versão do plano a que a sessão pertence; nulo só em sessões anteriores à V805. */
	public UUID getPlanId() {
		return planId;
	}

	public UUID getPeriodizationId() {
		return this.periodizationId;
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

	public int getWeekNumber() {
		return this.weekNumber;
	}

	public int getDayOfWeek() {
		return this.dayOfWeek;
	}

	public TrainingStatus getStatus() {
		return this.status;
	}

	public List<ExercisePrescription> getExercises() {
		return Collections.unmodifiableList(this.exercises);
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
