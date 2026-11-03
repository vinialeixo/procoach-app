package com.fitness.procoach.library.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fitness.procoach.library.domain.Exercise;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.ExerciseRepository;
import com.fitness.procoach.library.domain.Equipment;
import com.fitness.procoach.library.domain.MuscleGroup;

/**
 * Application service da Biblioteca de Exercícios. Orquestra o domínio e a porta
 * de persistência. Não contém anotações Spring (framework-agnostic).
 */
public class ExerciseApplicationService {

	private final ExerciseRepository repository;
	private final Clock clock;

	public ExerciseApplicationService(ExerciseRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	public Exercise create(CreateExerciseCommand command) {
		Exercise exercise = Exercise.builder()
				.name(command.name())
				.muscleGroups(command.muscleGroups())
				.equipment(command.equipment())
				.execution(command.execution())
				.commonErrors(command.commonErrors())
				.progressions(command.progressions())
				.regressions(command.regressions())
				.contraindications(command.contraindications())
				.references(command.references())
				.createdBy(command.createdBy())
				.createdAt(clock.instant())
				.updatedAt(clock.instant())
				.build();
		return repository.save(exercise);
	}

	/**
	 * Atualiza um exercício. Só o autor ({@code createdBy}) pode alterar; para
	 * qualquer outro usuário o exercício aparece como inexistente (404), sem
	 * vazar sua existência.
	 */
	public Exercise update(UUID id, UpdateExerciseCommand command, UUID requesterId) {
		Exercise exercise = findOwned(id, requesterId);
		exercise.update(command.name(), command.muscleGroups(), command.equipment(),
				command.execution(), command.commonErrors(), command.progressions(),
				command.regressions(), command.contraindications(), command.references(),
				clock.instant());
		return repository.save(exercise);
	}

	/** Soft-delete: marca como excluído, sem remoção física. */
	public void delete(UUID id, UUID requesterId) {
		Exercise exercise = findOwned(id, requesterId);
		exercise.delete(clock.instant());
		repository.save(exercise);
	}

	private Exercise findOwned(UUID id, UUID requesterId) {
		Exercise exercise = repository.findById(id)
				.orElseThrow(() -> new ExerciseNotFoundException(id));
		if (!exercise.getCreatedBy().equals(requesterId)) {
			throw new ExerciseNotFoundException(id);
		}
		return exercise;
	}

	public Exercise findById(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new ExerciseNotFoundException(id));
	}

	public List<Exercise> findAll() {
		return repository.findAll();
	}

	public List<Exercise> search(String name, MuscleGroup muscleGroup) {
		return repository.search(name, muscleGroup);
	}

	public record CreateExerciseCommand(String name, List<MuscleGroup> muscleGroups,
			Equipment equipment, String execution, List<String> commonErrors,
			List<String> progressions, List<String> regressions, List<String> contraindications,
			List<ExerciseReference> references, UUID createdBy) {
	}

	public record UpdateExerciseCommand(String name, List<MuscleGroup> muscleGroups,
			Equipment equipment, String execution, List<String> commonErrors,
			List<String> progressions, List<String> regressions, List<String> contraindications,
			List<ExerciseReference> references) {
	}
}
