package com.fitness.procoach.library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ExerciseTest {

	private final UUID coachId = UUID.randomUUID();

	private Exercise validExercise() {
		return Exercise.builder()
				.name("Supino Reto")
				.muscleGroups(List.of(MuscleGroup.PEITO, MuscleGroup.TRICEPS))
				.equipment(Equipment.BARRA_LIVRE)
				.execution("Deite no banco, desça a barra até o peito e empurre.")
				.commonErrors(List.of("Arqueamento excessivo da lombar"))
				.progressions(List.of("Aumentar carga progressivamente"))
				.regressions(List.of("Usar barra mais leve"))
				.contraindications(List.of("Lesão de ombro aguda"))
				.references(List.of(new ExerciseReference("Schoenfeld", "Science and Development of Muscle Hypertrophy", "Human Kinetics", 2020)))
				.createdBy(coachId)
				.build();
	}

	@Test
	void criaExercicioComTodosOsCamposCientificos() {
		Exercise e = validExercise();
		assertEquals("Supino Reto", e.getName());
		assertEquals(2, e.getMuscleGroups().size());
		assertEquals(Equipment.BARRA_LIVRE, e.getEquipment());
		assertFalse(e.getExecution().isBlank());
		assertEquals(1, e.getCommonErrors().size());
		assertEquals(1, e.getProgressions().size());
		assertEquals(1, e.getRegressions().size());
		assertEquals(1, e.getContraindications().size());
		assertEquals(1, e.getReferences().size());
		assertEquals(coachId, e.getCreatedBy());
		assertFalse(e.isDeleted());
	}

	@Test
	void rejeitaNomeObrigatorio() {
		assertThrows(IllegalArgumentException.class, () -> Exercise.builder()
				.name(" ")
				.muscleGroups(List.of(MuscleGroup.PEITO))
				.equipment(Equipment.BARRA_LIVRE)
				.execution("execução")
				.createdBy(coachId)
				.build());
	}

	@Test
	void rejeitaGruposMuscularesVazios() {
		assertThrows(IllegalArgumentException.class, () -> Exercise.builder()
				.name("Supino")
				.muscleGroups(List.of())
				.equipment(Equipment.BARRA_LIVRE)
				.execution("execução")
				.createdBy(coachId)
				.build());
	}

	@Test
	void rejeitaEquipamentoNulo() {
		assertThrows(NullPointerException.class, () -> Exercise.builder()
				.name("Supino")
				.muscleGroups(List.of(MuscleGroup.PEITO))
				.equipment(null)
				.execution("execução")
				.createdBy(coachId)
				.build());
	}

	@Test
	void rejeitaExecucaoObrigatoria() {
		assertThrows(IllegalArgumentException.class, () -> Exercise.builder()
				.name("Supino")
				.muscleGroups(List.of(MuscleGroup.PEITO))
				.equipment(Equipment.BARRA_LIVRE)
				.execution("")
				.createdBy(coachId)
				.build());
	}

	@Test
	void rejeitaCreatedByObrigatorio() {
		assertThrows(NullPointerException.class, () -> Exercise.builder()
				.name("Supino")
				.muscleGroups(List.of(MuscleGroup.PEITO))
				.equipment(Equipment.BARRA_LIVRE)
				.execution("execução")
				.build());
	}

	@Test
	void editarCriaNovaVersaoEPreservaAAnterior() {
		Exercise v1 = validExercise();
		Instant now = Instant.now();

		Exercise v2 = v1.createNewVersion("Supino Inclinado", List.of(MuscleGroup.PEITO), Equipment.HALTERES,
				"nova execução", List.of("erro novo"), List.of("progressão nova"),
				List.of("regressão nova"), List.of("contraindicação nova"),
				List.of(new ExerciseReference("Autor", "Título", "Fonte", 2021)), now);

		assertNotEquals(v1.getId(), v2.getId());
		assertEquals(2, v2.getVersion());
		assertEquals(v1.getId(), v2.getPreviousVersionId());
		assertEquals(coachId, v2.getCreatedBy());
		assertEquals("Supino Inclinado", v2.getName());
		assertEquals(Equipment.HALTERES, v2.getEquipment());
		assertFalse(v2.isSuperseded());
		// A versão anterior fica congelada, com o conteúdo original.
		assertTrue(v1.isSuperseded());
		assertEquals("Supino Reto", v1.getName());
		assertEquals(Equipment.BARRA_LIVRE, v1.getEquipment());
		assertEquals(1, v1.getVersion());
	}

	@Test
	void versaoSubstituidaNaoPodeSerEditadaNemRemovida() {
		Exercise v1 = validExercise();
		v1.createNewVersion("Supino Inclinado", List.of(MuscleGroup.PEITO), Equipment.HALTERES, "execução",
				List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now());

		assertThrows(IllegalStateException.class, () -> v1.createNewVersion("Outro", List.of(MuscleGroup.PEITO),
				Equipment.HALTERES, "execução", List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now()));
		assertThrows(IllegalStateException.class, () -> v1.delete(Instant.now()));
	}

	@Test
	void exercicioRemovidoNaoGeraNovaVersao() {
		Exercise e = validExercise();
		e.delete(Instant.now());

		assertThrows(IllegalStateException.class, () -> e.createNewVersion("Outro", List.of(MuscleGroup.PEITO),
				Equipment.HALTERES, "execução", List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now()));
	}

	@Test
	void novaVersaoInvalidaNaoCongelaAAnterior() {
		Exercise v1 = validExercise();

		assertThrows(IllegalArgumentException.class, () -> v1.createNewVersion(" ", List.of(MuscleGroup.PEITO),
				Equipment.HALTERES, "execução", List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now()));

		assertFalse(v1.isSuperseded());
	}

	@Test
	void exercicioNovoComecaNaVersaoUmSemAnterior() {
		Exercise e = validExercise();

		assertEquals(1, e.getVersion());
		assertNull(e.getPreviousVersionId());
		assertFalse(e.isSuperseded());
	}

	@Test
	void softDeleteMarcaComoExcluido() {
		Exercise e = validExercise();
		e.delete(Instant.now());
		assertTrue(e.isDeleted());
	}
}
