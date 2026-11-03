package com.fitness.procoach.library.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root da Biblioteca de Exercícios.
 * Framework-agnostic (sem anotações Spring/JPA).
 *
 * <p>Representa um exercício com campos científicos completos (US-036):
 * nome, grupos musculares, equipamento, execução, erros comuns, progressões,
 * regressões, contraindicações e referências bibliográficas.</p>
 */
public class Exercise {

	private final UUID id;
	private String name;
	private List<MuscleGroup> muscleGroups;
	private Equipment equipment;
	private String execution;
	private List<String> commonErrors;
	private List<String> progressions;
	private List<String> regressions;
	private List<String> contraindications;
	private List<ExerciseReference> references;
	private final UUID createdBy;
	private final Instant createdAt;
	private Instant updatedAt;
	private boolean deleted;

	private Exercise(Builder builder) {
		this.id = builder.id;
		this.name = builder.name;
		this.muscleGroups = builder.muscleGroups;
		this.equipment = builder.equipment;
		this.execution = builder.execution;
		this.commonErrors = builder.commonErrors;
		this.progressions = builder.progressions;
		this.regressions = builder.regressions;
		this.contraindications = builder.contraindications;
		this.references = builder.references;
		this.createdBy = builder.createdBy;
		this.createdAt = builder.createdAt;
		this.updatedAt = builder.updatedAt;
		this.deleted = builder.deleted;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Atualiza os campos editáveis do exercício. Nunca altera id, createdBy ou createdAt.
	 */
	public void update(String name, List<MuscleGroup> muscleGroups, Equipment equipment,
			String execution, List<String> commonErrors, List<String> progressions,
			List<String> regressions, List<String> contraindications,
			List<ExerciseReference> references, Instant now) {
		this.name = requireText(name, "name");
		this.muscleGroups = requireNonEmpty(muscleGroups, "muscleGroups");
		this.equipment = Objects.requireNonNull(equipment, "equipment é obrigatório");
		this.execution = requireText(execution, "execution");
		this.commonErrors = copy(commonErrors);
		this.progressions = copy(progressions);
		this.regressions = copy(regressions);
		this.contraindications = copy(contraindications);
		this.references = copyRefs(references);
		this.updatedAt = Objects.requireNonNull(now, "now é obrigatório");
	}

	/** Soft-delete: marca como excluído sem remoção física. */
	public void delete(Instant now) {
		this.deleted = true;
		this.updatedAt = Objects.requireNonNull(now, "now é obrigatório");
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<MuscleGroup> getMuscleGroups() {
		return Collections.unmodifiableList(muscleGroups);
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public String getExecution() {
		return execution;
	}

	public List<String> getCommonErrors() {
		return Collections.unmodifiableList(commonErrors);
	}

	public List<String> getProgressions() {
		return Collections.unmodifiableList(progressions);
	}

	public List<String> getRegressions() {
		return Collections.unmodifiableList(regressions);
	}

	public List<String> getContraindications() {
		return Collections.unmodifiableList(contraindications);
	}

	public List<ExerciseReference> getReferences() {
		return Collections.unmodifiableList(references);
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public boolean isDeleted() {
		return deleted;
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " é obrigatório");
		}
		return value.trim();
	}

	private static <T> List<T> requireNonEmpty(List<T> value, String field) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException(field + " é obrigatório");
		}
		return copy(value);
	}

	private static <T> List<T> copy(List<T> value) {
		return value == null ? new ArrayList<>() : new ArrayList<>(value);
	}

	private static List<ExerciseReference> copyRefs(List<ExerciseReference> value) {
		return value == null ? new ArrayList<>() : new ArrayList<>(value);
	}

	public static class Builder {
		private UUID id = UUID.randomUUID();
		private String name;
		private List<MuscleGroup> muscleGroups;
		private Equipment equipment;
		private String execution;
		private List<String> commonErrors = new ArrayList<>();
		private List<String> progressions = new ArrayList<>();
		private List<String> regressions = new ArrayList<>();
		private List<String> contraindications = new ArrayList<>();
		private List<ExerciseReference> references = new ArrayList<>();
		private UUID createdBy;
		private Instant createdAt = Instant.now();
		private Instant updatedAt = Instant.now();
		private boolean deleted;

		public Builder id(UUID id) {
			this.id = id;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder muscleGroups(List<MuscleGroup> muscleGroups) {
			this.muscleGroups = muscleGroups;
			return this;
		}

		public Builder equipment(Equipment equipment) {
			this.equipment = equipment;
			return this;
		}

		public Builder execution(String execution) {
			this.execution = execution;
			return this;
		}

		public Builder commonErrors(List<String> commonErrors) {
			this.commonErrors = commonErrors;
			return this;
		}

		public Builder progressions(List<String> progressions) {
			this.progressions = progressions;
			return this;
		}

		public Builder regressions(List<String> regressions) {
			this.regressions = regressions;
			return this;
		}

		public Builder contraindications(List<String> contraindications) {
			this.contraindications = contraindications;
			return this;
		}

		public Builder references(List<ExerciseReference> references) {
			this.references = references;
			return this;
		}

		public Builder createdBy(UUID createdBy) {
			this.createdBy = createdBy;
			return this;
		}

		public Builder createdAt(Instant createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		public Builder updatedAt(Instant updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		public Builder deleted(boolean deleted) {
			this.deleted = deleted;
			return this;
		}

		public Exercise build() {
			Exercise exercise = new Exercise(this);
			// valida campos obrigatórios no build
			exercise.name = requireText(exercise.name, "name");
			exercise.muscleGroups = requireNonEmpty(exercise.muscleGroups, "muscleGroups");
			exercise.equipment = Objects.requireNonNull(exercise.equipment, "equipment é obrigatório");
			exercise.execution = requireText(exercise.execution, "execution");
			if (exercise.createdBy == null) {
				throw new NullPointerException("createdBy é obrigatório");
			}
			return exercise;
		}
	}
}
