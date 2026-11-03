package com.fitness.procoach.library.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência do agregado Exercise. Framework-agnostic.
 * Implementada na camada de infrastructure (Spring Data JPA).
 */
public interface ExerciseRepository {

	Exercise save(Exercise exercise);

	Optional<Exercise> findById(UUID id);

	List<Exercise> findAll();

	/** Busca por nome (parcial, case-insensitive) ou grupo muscular. */
	List<Exercise> search(String name, MuscleGroup muscleGroup);

	void delete(Exercise exercise);

	/** Versão que substituiu {@code previousVersionId}, mesmo que removida; vazio se não há sucessora. */
	Optional<Exercise> findSuccessor(UUID previousVersionId);
}
