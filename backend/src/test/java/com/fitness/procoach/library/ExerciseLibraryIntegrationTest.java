package com.fitness.procoach.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

	private ExerciseApplicationService.UpdateExerciseCommand updateCommand(String name, Equipment equipment) {
		return new ExerciseApplicationService.UpdateExerciseCommand(name, List.of(MuscleGroup.PEITO), equipment,
				"nova execução", List.of("erro novo"), List.of("progressão nova"), List.of("regressão nova"),
				List.of("contraindicação nova"), List.of(new ExerciseReference("Autor", "Título", "Fonte", 2021)));
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
	void editarCriaNovaVersaoEDeixaAAnteriorIntacta() {
		Exercise created = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));

		Exercise updated = service.update(created.getId(), updateCommand("Supino Inclinado", Equipment.HALTERES),
				coachId);

		assertNotEquals(created.getId(), updated.getId());
		assertEquals("Supino Inclinado", updated.getName());
		assertEquals(Equipment.HALTERES, updated.getEquipment());
		assertEquals(2, updated.getVersion());
		assertEquals(created.getId(), updated.getPreviousVersionId());
		// A versão antiga continua legível por id, com o conteúdo original, mas congelada.
		Exercise old = service.findById(created.getId());
		assertEquals("Supino Reto", old.getName());
		assertEquals(Equipment.BARRA_LIVRE, old.getEquipment());
		assertTrue(old.isSuperseded());
	}

	@Test
	void buscaEListagemMostramSoAVersaoCorrente() {
		Exercise created = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));
		Exercise updated = service.update(created.getId(), updateCommand("Supino Inclinado", Equipment.HALTERES),
				coachId);

		List<Exercise> byName = service.search("supino", null);
		assertEquals(1, byName.size());
		assertEquals(updated.getId(), byName.get(0).getId());
		assertTrue(service.findAll().stream().noneMatch(e -> e.getId().equals(created.getId())));
	}

	@Test
	void versaoAntigaNaoPodeSerEditadaNovamente() {
		Exercise created = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));
		service.update(created.getId(), updateCommand("Supino Inclinado", Equipment.HALTERES), coachId);

		assertThrows(IllegalStateException.class, () -> service.update(created.getId(),
				updateCommand("Outra edição", Equipment.BARRA_LIVRE), coachId));
	}

	@Test
	void cadeiaDeVersoesAvancaAPartirDaCorrente() {
		Exercise v1 = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));
		Exercise v2 = service.update(v1.getId(), updateCommand("Supino Inclinado", Equipment.HALTERES), coachId);
		Exercise v3 = service.update(v2.getId(), updateCommand("Supino Declinado", Equipment.BARRA_LIVRE), coachId);

		assertEquals(3, v3.getVersion());
		assertEquals(v2.getId(), v3.getPreviousVersionId());
		assertTrue(service.findById(v2.getId()).isSuperseded());
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

	// ---- histórico de alterações (T-014 redefinida) ----

	@Autowired
	private com.fitness.procoach.audit.application.AuditService auditService;

	@Test
	void edicaoDoExercicioFicaNoHistoricoComAntesEDepois() {
		Exercise created = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));

		Exercise updated = service.update(created.getId(), updateCommand("Supino Inclinado", Equipment.HALTERES),
				coachId);

		assertEquals(List.of("EXERCISE_CREATED"),
				auditService.findByAggregate(created.getId()).stream().map(l -> l.getEventType()).toList());
		var log = auditService.findByAggregate(updated.getId());
		assertEquals(List.of("EXERCISE_VERSIONED"), log.stream().map(l -> l.getEventType()).toList());
		String data = log.get(0).getData();
		assertTrue(data.contains("\"version\":2"), data);
		assertTrue(data.contains("\"previousVersionId\":\"" + created.getId() + "\""), data);
		assertTrue(data.contains("\"nameBefore\":\"Supino Reto\""), data);
		assertTrue(data.contains("\"nameAfter\":\"Supino Inclinado\""), data);
		assertTrue(data.contains("\"equipmentAfter\":\"HALTERES\""), data);
		assertEquals(coachId, log.get(0).getActorId());
	}

	@Test
	void editarSemMudarNadaRegistraListaDeAlteracoesVazia() {
		Exercise created = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));

		Exercise updated = service.update(created.getId(),
				new ExerciseApplicationService.UpdateExerciseCommand("Supino Reto", List.of(MuscleGroup.PEITO),
						Equipment.BARRA_LIVRE, "Deite no banco e empurre a barra.", List.of("erro comum"),
						List.of("progressão"), List.of("regressão"), List.of("contraindicação"),
						List.of(new ExerciseReference("Schoenfeld", "Hypertrophy", "Human Kinetics", 2020))),
				coachId);

		var log = auditService.findByAggregate(updated.getId());
		assertTrue(log.get(0).getData().contains("\"changed\":\"\""), log.get(0).getData());
	}

	@Test
	void remocaoDoExercicioFicaNoHistorico() {
		Exercise created = service.create(command("Levantamento Terra", List.of(MuscleGroup.COSTAS)));

		service.delete(created.getId(), coachId);

		var log = auditService.findByAggregate(created.getId());
		assertEquals("EXERCISE_DELETED", log.get(log.size() - 1).getEventType());
	}

	// ---- status de versão para o módulo de treino (T-066) ----

	@Test
	void statusDeVersaoIndicaAVersaoCorrenteAPartirDeQualquerVersaoAntiga() {
		Exercise v1 = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));
		Exercise v2 = service.update(v1.getId(), updateCommand("Supino Inclinado", Equipment.HALTERES), coachId);
		Exercise v3 = service.update(v2.getId(), updateCommand("Supino Declinado", Equipment.BARRA_LIVRE), coachId);

		var statuses = service.versionStatuses(List.of(v1.getId(), v2.getId(), v3.getId(), UUID.randomUUID()));

		assertEquals(3, statuses.size());
		assertTrue(statuses.get(v1.getId()).superseded());
		assertEquals(v3.getId(), statuses.get(v1.getId()).currentVersionId());
		assertEquals(v3.getId(), statuses.get(v2.getId()).currentVersionId());
		assertEquals(3, statuses.get(v3.getId()).version());
		assertFalse(statuses.get(v3.getId()).superseded());
		assertFalse(statuses.get(v3.getId()).newerVersionAvailable());
	}

	@Test
	void statusDeVersaoNaoSugereVersaoCorrenteRemovida() {
		Exercise v1 = service.create(command("Supino Reto", List.of(MuscleGroup.PEITO)));
		Exercise v2 = service.update(v1.getId(), updateCommand("Supino Inclinado", Equipment.HALTERES), coachId);
		service.delete(v2.getId(), coachId);

		var status = service.versionStatuses(List.of(v1.getId())).get(v1.getId());

		assertTrue(status.superseded());
		assertFalse(status.newerVersionAvailable());
	}
}
