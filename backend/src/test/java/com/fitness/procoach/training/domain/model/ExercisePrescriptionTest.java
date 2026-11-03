package com.fitness.procoach.training.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExercisePrescriptionTest {

	@Test
	void createsPrescriptionWithAllParameters() {
		ExercisePrescription p = ExercisePrescription.create(UUID.randomUUID(), "Supino Reto", false, 0, 4, 10,
				new BigDecimal("80.00"), IntensityType.PERCENT_1RM, new BigDecimal("75.0"), 90, "Cadência 2-0-2");

		assertThat(p.getId()).isNotNull();
		assertThat(p.getExerciseName()).isEqualTo("Supino Reto");
		assertThat(p.getSets()).isEqualTo(4);
		assertThat(p.getRepetitions()).isEqualTo(10);
		assertThat(p.getLoad()).isEqualByComparingTo(new BigDecimal("80.00"));
		assertThat(p.getIntensityType()).isEqualTo(IntensityType.PERCENT_1RM);
		assertThat(p.getIntensityValue()).isEqualByComparingTo(new BigDecimal("75.0"));
		assertThat(p.getRestSeconds()).isEqualTo(90);
		assertThat(p.getNotes()).isEqualTo("Cadência 2-0-2");
	}

	@Test
	void createsCustomExercise() {
		ExercisePrescription p = ExercisePrescription.create(null, "Exercício Personalizado", true, 0, 3, 12,
				new BigDecimal("50.00"), IntensityType.RPE, new BigDecimal("8.0"), 60, null);

		assertThat(p.isCustomExercise()).isTrue();
		assertThat(p.getExerciseId()).isNull();
	}

	@Test
	void rejectsZeroSets() {
		assertThatThrownBy(() -> ExercisePrescription.create(UUID.randomUUID(), "Ex", false, 0, 0, 10,
				new BigDecimal("50"), IntensityType.RPE, new BigDecimal("7"), 60, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("sets must be a positive number");
	}

	@Test
	void rejectsNegativeRepetitions() {
		assertThatThrownBy(() -> ExercisePrescription.create(UUID.randomUUID(), "Ex", false, 0, 3, -1,
				new BigDecimal("50"), IntensityType.RPE, new BigDecimal("7"), 60, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("repetitions must be a positive number");
	}

	@Test
	void rejectsZeroLoad() {
		assertThatThrownBy(() -> ExercisePrescription.create(UUID.randomUUID(), "Ex", false, 0, 3, 10,
				BigDecimal.ZERO, IntensityType.RPE, new BigDecimal("7"), 60, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("load must be a positive number");
	}

	@Test
	void rejectsNegativeIntensityValue() {
		assertThatThrownBy(() -> ExercisePrescription.create(UUID.randomUUID(), "Ex", false, 0, 3, 10,
				new BigDecimal("50"), IntensityType.RPE, new BigDecimal("-1"), 60, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("intensityValue must be a positive number");
	}

	@Test
	void rejectsZeroRestSeconds() {
		assertThatThrownBy(() -> ExercisePrescription.create(UUID.randomUUID(), "Ex", false, 0, 3, 10,
				new BigDecimal("50"), IntensityType.RPE, new BigDecimal("7"), 0, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("restSeconds must be a positive number");
	}

	@Test
	void rejectsBlankExerciseName() {
		assertThatThrownBy(() -> ExercisePrescription.create(UUID.randomUUID(), "", false, 0, 3, 10,
				new BigDecimal("50"), IntensityType.RPE, new BigDecimal("7"), 60, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("exerciseName must not be blank");
	}

	@Test
	void rejectsNullIntensityType() {
		assertThatThrownBy(() -> ExercisePrescription.create(UUID.randomUUID(), "Ex", false, 0, 3, 10,
				new BigDecimal("50"), null, new BigDecimal("7"), 60, null))
			.isInstanceOf(NullPointerException.class);
	}

}
