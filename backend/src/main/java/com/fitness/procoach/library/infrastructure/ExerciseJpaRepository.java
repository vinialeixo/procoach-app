package com.fitness.procoach.library.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitness.procoach.library.domain.MuscleGroup;

/** Repositório Spring Data JPA da entidade ExerciseJpaEntity. */
public interface ExerciseJpaRepository extends JpaRepository<ExerciseJpaEntity, UUID> {

	@Query("""
			select e from ExerciseJpaEntity e
			where e.deleted = false
			  and lower(e.name) like lower(:pattern)
			  and (:muscleGroup is null or :muscleGroup member of e.muscleGroups)
			order by e.name
			""")
	List<ExerciseJpaEntity> search(@Param("pattern") String pattern, @Param("muscleGroup") MuscleGroup muscleGroup);

	@Query("select e from ExerciseJpaEntity e where e.deleted = false order by e.name")
	List<ExerciseJpaEntity> findAllActive();

	Optional<ExerciseJpaEntity> findByIdAndDeletedFalse(UUID id);
}
