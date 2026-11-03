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
	private final int version;
	private final UUID previousVersionId;
	private boolean superseded;

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
		this.version = builder.version;
		this.previousVersionId = builder.previousVersionId;
		this.superseded = builder.superseded;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Edição versionada (T-066): o exercício nunca é sobrescrito. Esta versão passa a
	 * ser a anterior (somente leitura, {@code superseded}) e a nova, com id novo,
	 * carrega os campos editados — treinos já prescritos seguem apontando para a
	 * versão que o coach usou. Mantém {@code createdBy}; só a versão corrente pode
	 * ser editada.
	 */
	public Exercise createNewVersion(String name, List<MuscleGroup> muscleGroups, Equipment equipment,
			String execution, List<String> commonErrors, List<String> progressions,
			List<String> regressions, List<String> contraindications,
			List<ExerciseReference> references, Instant now) {
		if (deleted) {
			throw new IllegalStateException("Cannot version a deleted exercise");
		}
		if (superseded) {
			throw new IllegalStateException("Exercise version has been superseded; edit the current version");
		}
		Objects.requireNonNull(now, "now é obrigatório");
		// A nova versão é validada antes de congelar esta.
		Exercise next = builder()
				.name(name)
				.muscleGroups(muscleGroups)
				.equipment(equipment)
				.execution(execution)
				.commonErrors(copy(commonErrors))
				.progressions(copy(progressions))
				.regressions(copy(regressions))
				.contraindications(copy(contraindications))
				.references(copyRefs(references))
				.createdBy(createdBy)
				.createdAt(now)
				.updatedAt(now)
				.version(version + 1)
				.previousVersionId(id)
				.build();
		this.superseded = true;
		this.updatedAt = now;
		return next;
	}

	/** Soft-delete: marca como excluído sem remoção física. */
	public void delete(Instant now) {
		if (superseded) {
			throw new IllegalStateException("Exercise version has been superseded; delete the current version");
		}
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

	public int getVersion() {
		return version;
	}

	public UUID getPreviousVersionId() {
		return previousVersionId;
	}

	/** {@code true} quando uma versão mais nova já substituiu esta (somente leitura). */
	public boolean isSuperseded() {
		return superseded;
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
		private int version = 1;
		private UUID previousVersionId;
		private boolean superseded;

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

		public Builder version(int version) {
			this.version = version;
			return this;
		}

		public Builder previousVersionId(UUID previousVersionId) {
			this.previousVersionId = previousVersionId;
			return this;
		}

		public Builder superseded(boolean superseded) {
			this.superseded = superseded;
			return this;
		}

		public Exercise build() {
			Exercise exercise = new Exercise(this);
			// valida campos obrigatórios no build
			exercise.name = requireText(exercise.name, "name");
			exercise.muscleGroups = requireNonEmpty(exercise.muscleGroups, "muscleGroups");
			exercise.equipment = Objects.requireNonNull(exercise.equipment, "equipment é obrigatório");
			exercise.execution = requireText(exercise.execution, "execution");
			if (exercise.version < 1) {
				throw new IllegalArgumentException("version deve ser >= 1");
			}
			if (exercise.createdBy == null) {
				throw new NullPointerException("createdBy é obrigatório");
			}
			return exercise;
		}
	}
}
