package com.fitness.procoach.library.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fitness.procoach.library.domain.Exercise;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.ExerciseRepository;
import com.fitness.procoach.library.domain.MuscleGroup;

/** Adapter que implementa a porta ExerciseRepository usando Spring Data JPA. */
@Repository
public class ExerciseRepositoryAdapter implements ExerciseRepository {

	private final ExerciseJpaRepository jpaRepository;

	public ExerciseRepositoryAdapter(ExerciseJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Exercise save(Exercise exercise) {
		return toDomain(jpaRepository.save(toJpa(exercise)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Exercise> findById(UUID id) {
		return jpaRepository.findByIdAndDeletedFalse(id).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Exercise> findAll() {
		return jpaRepository.findAllActive().stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Exercise> search(String name, MuscleGroup muscleGroup) {
		String pattern = (name == null || name.isBlank()) ? "%" : "%" + name.trim() + "%";
		return jpaRepository.search(pattern, muscleGroup).stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional
	public void delete(Exercise exercise) {
		jpaRepository.save(toJpa(exercise));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Exercise> findSuccessor(UUID previousVersionId) {
		return jpaRepository.findByPreviousVersionId(previousVersionId).map(this::toDomain);
	}

	private ExerciseJpaEntity toJpa(Exercise e) {
		ExerciseJpaEntity jpa = new ExerciseJpaEntity();
		jpa.setId(e.getId());
		jpa.setName(e.getName());
		jpa.setMuscleGroups(e.getMuscleGroups());
		jpa.setEquipment(e.getEquipment());
		jpa.setExecution(e.getExecution());
		jpa.setCommonErrors(e.getCommonErrors());
		jpa.setProgressions(e.getProgressions());
		jpa.setRegressions(e.getRegressions());
		jpa.setContraindications(e.getContraindications());
		jpa.setReferences(e.getReferences().stream().map(jpa::fromDomainReference).toList());
		jpa.setCreatedBy(e.getCreatedBy());
		jpa.setCreatedAt(e.getCreatedAt());
		jpa.setUpdatedAt(e.getUpdatedAt());
		jpa.setDeleted(e.isDeleted());
		jpa.setVersion(e.getVersion());
		jpa.setPreviousVersionId(e.getPreviousVersionId());
		jpa.setSuperseded(e.isSuperseded());
		return jpa;
	}

	private Exercise toDomain(ExerciseJpaEntity jpa) {
		return Exercise.builder()
				.id(jpa.getId())
				.name(jpa.getName())
				.muscleGroups(jpa.getMuscleGroups())
				.equipment(jpa.getEquipment())
				.execution(jpa.getExecution())
				.commonErrors(jpa.getCommonErrors())
				.progressions(jpa.getProgressions())
				.regressions(jpa.getRegressions())
				.contraindications(jpa.getContraindications())
				.references(jpa.getReferences().stream().map(jpa::toDomainReference).toList())
				.createdBy(jpa.getCreatedBy())
				.createdAt(jpa.getCreatedAt())
				.updatedAt(jpa.getUpdatedAt())
				.deleted(jpa.isDeleted())
				.version(jpa.getVersion())
				.previousVersionId(jpa.getPreviousVersionId())
				.superseded(jpa.isSuperseded())
				.build();
	}
}
