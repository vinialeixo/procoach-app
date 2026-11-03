package com.fitness.procoach.library.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

import com.fitness.procoach.library.domain.Exercise;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.ExerciseRepository;
import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.library.domain.Equipment;
import com.fitness.procoach.library.domain.MuscleGroup;

/**
 * Application service da Biblioteca de Exercícios. Orquestra o domínio e a porta
 * de persistência. Não contém anotações Spring (framework-agnostic).
 */
public class ExerciseApplicationService {

	private final ExerciseRepository repository;
	private final Clock clock;
	private final AuditService auditService;

	public ExerciseApplicationService(ExerciseRepository repository, Clock clock, AuditService auditService) {
		this.auditService = auditService;
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional
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
		Exercise saved = repository.save(exercise);
		auditService.record("Exercise", saved.getId(), null, "EXERCISE_CREATED", command.createdBy(),
				AuditData.of("name", saved.getName(), "equipment", saved.getEquipment()));
		return saved;
	}

	/**
	 * Atualiza um exercício. Só o autor ({@code createdBy}) pode alterar; para
	 * qualquer outro usuário o exercício aparece como inexistente (404), sem
	 * vazar sua existência.
	 */
	@Transactional
	public Exercise update(UUID id, UpdateExerciseCommand command, UUID requesterId) {
		Exercise current = findOwned(id, requesterId);
		Snapshot before = Snapshot.of(current);
		Exercise next = current.createNewVersion(command.name(), command.muscleGroups(), command.equipment(),
				command.execution(), command.commonErrors(), command.progressions(),
				command.regressions(), command.contraindications(), command.references(),
				clock.instant());
		repository.save(current);
		Exercise saved = repository.save(next);
		auditService.record("Exercise", saved.getId(), null, "EXERCISE_VERSIONED", requesterId,
				before.changes(Snapshot.of(saved), saved.getVersion(), current.getId()));
		return saved;
	}

	/** Soft-delete: marca como excluído, sem remoção física. */
	@Transactional
	public void delete(UUID id, UUID requesterId) {
		Exercise exercise = findOwned(id, requesterId);
		exercise.delete(clock.instant());
		repository.save(exercise);
		auditService.record("Exercise", exercise.getId(), null, "EXERCISE_DELETED", requesterId,
				AuditData.of("name", exercise.getName()));
	}

	/** Foto dos campos editáveis, para registrar no histórico o que mudou na edição. */
	private record Snapshot(String name, List<MuscleGroup> muscleGroups, Equipment equipment, String execution,
			List<String> commonErrors, List<String> progressions, List<String> regressions,
			List<String> contraindications, List<ExerciseReference> references) {

		static Snapshot of(Exercise e) {
			return new Snapshot(e.getName(), copy(e.getMuscleGroups()), e.getEquipment(), e.getExecution(),
					copy(e.getCommonErrors()), copy(e.getProgressions()), copy(e.getRegressions()),
					copy(e.getContraindications()), copy(e.getReferences()));
		}

		private static <T> List<T> copy(List<T> list) {
			return list == null ? List.of() : new ArrayList<>(list);
		}

		/**
		 * Payload do histórico: lista dos campos alterados e, para os de valor curto (nome,
		 * equipamento e grupos musculares), o antes e o depois. Textos longos (execução, erros
		 * comuns etc.) entram só pelo nome do campo.
		 */
		String changes(Snapshot after, int version, UUID previousVersionId) {
			List<String> changed = new ArrayList<>();
			List<Object> pairs = new ArrayList<>();
			if (!Objects.equals(name, after.name)) {
				changed.add("name");
				pairs.addAll(List.of("nameBefore", String.valueOf(name), "nameAfter", String.valueOf(after.name)));
			}
			if (!Objects.equals(equipment, after.equipment)) {
				changed.add("equipment");
				pairs.addAll(List.of("equipmentBefore", String.valueOf(equipment), "equipmentAfter",
						String.valueOf(after.equipment)));
			}
			if (!Objects.equals(muscleGroups, after.muscleGroups)) {
				changed.add("muscleGroups");
				pairs.addAll(List.of("muscleGroupsBefore", String.valueOf(muscleGroups), "muscleGroupsAfter",
						String.valueOf(after.muscleGroups)));
			}
			if (!Objects.equals(execution, after.execution)) {
				changed.add("execution");
			}
			if (!Objects.equals(commonErrors, after.commonErrors)) {
				changed.add("commonErrors");
			}
			if (!Objects.equals(progressions, after.progressions)) {
				changed.add("progressions");
			}
			if (!Objects.equals(regressions, after.regressions)) {
				changed.add("regressions");
			}
			if (!Objects.equals(contraindications, after.contraindications)) {
				changed.add("contraindications");
			}
			if (!Objects.equals(references, after.references)) {
				changed.add("references");
			}
			List<Object> all = new ArrayList<>(List.of("version", version, "previousVersionId",
					previousVersionId.toString(), "changed", String.join(",", changed)));
			all.addAll(pairs);
			return AuditData.of(all.toArray());
		}
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
