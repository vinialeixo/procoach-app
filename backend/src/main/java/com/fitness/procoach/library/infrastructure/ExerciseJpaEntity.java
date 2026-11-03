package com.fitness.procoach.library.infrastructure;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fitness.procoach.library.domain.Equipment;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.MuscleGroup;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/** Entidade JPA do agregado Exercise. Mapeada para a tabela library_exercise. */
@Entity
@Table(name = "library_exercise")
public class ExerciseJpaEntity {

	@Id
	@Column(name = "id", columnDefinition = "uuid")
	private UUID id;

	@Column(name = "name", nullable = false)
	private String name;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "library_exercise_muscle_group", joinColumns = @JoinColumn(name = "exercise_id"))
	@Column(name = "muscle_group")
	@Enumerated(EnumType.STRING)
	private List<MuscleGroup> muscleGroups = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "equipment", nullable = false)
	private Equipment equipment;

	@Column(name = "execution", nullable = false, length = 4000)
	private String execution;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "library_exercise_common_error", joinColumns = @JoinColumn(name = "exercise_id"))
	@Column(name = "common_error")
	private List<String> commonErrors = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "library_exercise_progression", joinColumns = @JoinColumn(name = "exercise_id"))
	@Column(name = "progression")
	private List<String> progressions = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "library_exercise_regression", joinColumns = @JoinColumn(name = "exercise_id"))
	@Column(name = "regression")
	private List<String> regressions = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "library_exercise_contraindication", joinColumns = @JoinColumn(name = "exercise_id"))
	@Column(name = "contraindication")
	private List<String> contraindications = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "library_exercise_reference", joinColumns = @JoinColumn(name = "exercise_id"))
	private List<ExerciseReferenceJpa> references = new ArrayList<>();

	@Column(name = "created_by", nullable = false, columnDefinition = "uuid")
	private UUID createdBy;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted", nullable = false)
	private boolean deleted;

	@Column(name = "version", nullable = false)
	private int version = 1;

	@Column(name = "previous_version_id", unique = true)
	private UUID previousVersionId;

	@Column(name = "superseded", nullable = false)
	private boolean superseded;

	protected ExerciseJpaEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<MuscleGroup> getMuscleGroups() {
		return muscleGroups;
	}

	public void setMuscleGroups(List<MuscleGroup> muscleGroups) {
		this.muscleGroups = muscleGroups;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	public String getExecution() {
		return execution;
	}

	public void setExecution(String execution) {
		this.execution = execution;
	}

	public List<String> getCommonErrors() {
		return commonErrors;
	}

	public void setCommonErrors(List<String> commonErrors) {
		this.commonErrors = commonErrors;
	}

	public List<String> getProgressions() {
		return progressions;
	}

	public void setProgressions(List<String> progressions) {
		this.progressions = progressions;
	}

	public List<String> getRegressions() {
		return regressions;
	}

	public void setRegressions(List<String> regressions) {
		this.regressions = regressions;
	}

	public List<String> getContraindications() {
		return contraindications;
	}

	public void setContraindications(List<String> contraindications) {
		this.contraindications = contraindications;
	}

	public List<ExerciseReferenceJpa> getReferences() {
		return references;
	}

	public void setReferences(List<ExerciseReferenceJpa> references) {
		this.references = references;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UUID createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	/** Converte para o agregado de domínio. */
	public ExerciseReference toDomainReference(ExerciseReferenceJpa ref) {
		return new ExerciseReference(ref.getAuthors(), ref.getTitle(), ref.getSource(), ref.getYear());
	}

	/** Converte um VO de domínio para a entidade embutida. */
	public ExerciseReferenceJpa fromDomainReference(ExerciseReference ref) {
		return new ExerciseReferenceJpa(ref.authors(), ref.title(), ref.source(), ref.year());
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public UUID getPreviousVersionId() {
		return previousVersionId;
	}

	public void setPreviousVersionId(UUID previousVersionId) {
		this.previousVersionId = previousVersionId;
	}

	public boolean isSuperseded() {
		return superseded;
	}

	public void setSuperseded(boolean superseded) {
		this.superseded = superseded;
	}
}
