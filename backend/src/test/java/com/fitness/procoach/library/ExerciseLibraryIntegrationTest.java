package com.fitness.procoach.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.fitness.procoach.TestcontainersConfiguration;
import com.fitness.procoach.library.application.ExerciseApplicationService;
import com.fitness.procoach.library.application.ExerciseNotFoundException;
import com.fitness.procoach.library.domain.Equipment;
import com.fitness.procoach.library.domain.Exercise;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.MuscleGroup;

/** Teste de integração do CRUD da Biblioteca de Exercícios (Testcontainers + PostgreSQL). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ExerciseLibraryIntegrationTest {

	@Autowired
	private ExerciseApplicationService service;

	private final UUID coachId = UUID.randomUUID();

	private ExerciseApplicationService.CreateExerciseCommand command(String name, List<MuscleGroup> groups) {
		return new ExerciseApplicationService.CreateExerciseCommand(name, groups, Equipment.BARRA_LIVRE,
				"Deite no banco e empurre a barra.", List.of("erro comum"), List.of("progressão"),
				List.of("regressão"), List.of("contraindicação"),
				List.of(new ExerciseReference("Schoenfeld", "Hypertrophy", "Human Kinetics", 2020)), coachId);
	}

	@Test
	void criaEBuscaExercicioPorNome() {
		Exercise created = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO, MuscleGroup.TRICEPS)));

		List<Exercise> byName = service.search("supino", null);
		assertEquals(1, byName.size());
		assertEquals("Supino Reto", byName.get(0).getName());
		assertEquals(created.getId(), byName.get(0).getId());
	}

	@Test
	void buscaPorGrupoMuscular() {
		service.create(command("Agachamento", List.of(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTEOS)));
		service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));

		List<Exercise> byGroup = service.search(null, MuscleGroup.QUADRICEPS);
		assertEquals(1, byGroup.size());
		assertEquals("Agachamento", byGroup.get(0).getName());
	}

	@Test
	void atualizaExercicio() {
		Exercise created = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));

		Exercise updated = service.update(created.getId(),
				new ExerciseApplicationService.UpdateExerciseCommand("Supino Inclinado",
						List.of(MuscleGroup.PEITO), Equipment.HALTERES, "nova execução",
						List.of("erro novo"), List.of("progressão nova"), List.of("regressão nova"),
						List.of("contraindicação nova"),
						List.of(new ExerciseReference("Autor", "Título", "Fonte", 2021))),
				coachId);

		assertEquals("Supino Inclinado", updated.getName());
		assertEquals(Equipment.HALTERES, updated.getEquipment());
		assertEquals(created.getId(), updated.getId());
	}

	@Test
	void softDeleteRemoveDaBusca() {
		Exercise created = service.create(command("Levantamento Terra", List.of(MuscleGroup.COSTAS)));

		service.delete(created.getId(), coachId);

		assertThrows(ExerciseNotFoundException.class, () -> service.findById(created.getId()));
		assertTrue(service.search("levantamento", null).isEmpty());
	}

	@Test
	void outroCoachNaoAtualizaNemRemoveExercicioAlheio() {
		Exercise created = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));
		UUID intruder = UUID.randomUUID();

		assertThrows(ExerciseNotFoundException.class, () -> service.update(created.getId(),
				new ExerciseApplicationService.UpdateExerciseCommand("Hackeado", List.of(MuscleGroup.PEITO),
						Equipment.BARRA_LIVRE, "x", List.of(), List.of(), List.of(), List.of(), List.of()),
				intruder));
		assertThrows(ExerciseNotFoundException.class, () -> service.delete(created.getId(), intruder));
		assertEquals("Supino Reto", service.findById(created.getId()).getName());
	}

	@Test
	void findByIdInexistenteLancaExcecao() {
		assertThrows(ExerciseNotFoundException.class, () -> service.findById(UUID.randomUUID()));
	}

	@Test
	void findAllRetornaSomenteAtivos() {
		Exercise a = service.create(command("Rosca Direta", List.of(MuscleGroup.BICEPS)));
		service.create(command("Tríceps Corda", List.of(MuscleGroup.TRICEPS)));
		service.delete(a.getId(), coachId);

		List<Exercise> all = service.findAll();
		assertFalse(all.isEmpty());
		assertTrue(all.stream().noneMatch(e -> e.getId().equals(a.getId())));
	}
}
