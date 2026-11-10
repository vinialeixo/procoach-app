package com.fitness.procoach.library.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitness.procoach.library.domain.Equipment;
import com.fitness.procoach.library.domain.Exercise;
import com.fitness.procoach.library.domain.ExerciseReference;
import com.fitness.procoach.library.domain.MuscleGroup;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExerciseResponseTest {

	@Test
	void mapsFieldsConsumedByTrainingLibraryToResponse() {
		UUID exerciseId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		UUID previousVersionId = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-11-03T12:00:00Z");
		Exercise exercise = Exercise.builder()
				.id(exerciseId)
				.name("Supino inclinado")
				.muscleGroups(List.of(MuscleGroup.PEITO, MuscleGroup.TRICEPS))
				.equipment(Equipment.HALTERES)
				.execution("Desça os halteres com controle.")
				.commonErrors(List.of("Perder o controle da descida"))
				.progressions(List.of("Aumentar a carga"))
				.regressions(List.of("Reduzir a carga"))
				.contraindications(List.of("Dor aguda no ombro"))
				.references(List.of(new ExerciseReference("Autor", "Estudo", "Revista", 2024)))
				.createdBy(coachId)
				.createdAt(createdAt)
				.updatedAt(createdAt)
				.version(2)
				.previousVersionId(previousVersionId)
				.build();

		ExerciseResponse response = ExerciseResponse.from(exercise);

		assertThat(response.id()).isEqualTo(exerciseId);
		assertThat(response.name()).isEqualTo("Supino inclinado");
		assertThat(response.muscleGroups()).containsExactly(MuscleGroup.PEITO, MuscleGroup.TRICEPS);
		assertThat(response.equipment()).isEqualTo(Equipment.HALTERES);
		assertThat(response.execution()).isEqualTo("Desça os halteres com controle.");
		assertThat(response.commonErrors()).containsExactly("Perder o controle da descida");
		assertThat(response.progressions()).containsExactly("Aumentar a carga");
		assertThat(response.regressions()).containsExactly("Reduzir a carga");
		assertThat(response.contraindications()).containsExactly("Dor aguda no ombro");
		assertThat(response.references()).hasSize(1);
		assertThat(response.createdBy()).isEqualTo(coachId);
		assertThat(response.version()).isEqualTo(2);
		assertThat(response.previousVersionId()).isEqualTo(previousVersionId);
		assertThat(response.superseded()).isFalse();
	}
}
