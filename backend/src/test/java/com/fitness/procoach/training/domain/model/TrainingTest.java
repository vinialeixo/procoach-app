package com.fitness.procoach.training.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingTest {

	private UUID periodizationId;

	private UUID athleteId;

	private UUID coachId;

	@BeforeEach
	void setUp() {
		this.periodizationId = UUID.randomUUID();
		this.athleteId = UUID.randomUUID();
		this.coachId = UUID.randomUUID();
	}

	@Test
	void createsTrainingLinkedToPeriodization() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", "Peito/Tríceps",
				1, 1);

		assertThat(t.getId()).isNotNull();
		assertThat(t.getPeriodizationId()).isEqualTo(this.periodizationId);
		assertThat(t.getAthleteId()).isEqualTo(this.athleteId);
		assertThat(t.getCoachId()).isEqualTo(this.coachId);
		assertThat(t.getStatus()).isEqualTo(TrainingStatus.DRAFT);
		assertThat(t.isDeleted()).isFalse();
		assertThat(t.getExercises()).isEmpty();
	}

	@Test
	void rejectsNullPeriodizationId() {
		assertThatThrownBy(
				() -> Training.create(null, this.athleteId, this.coachId, "Treino", null, 1, 1))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("periodizationId must not be null");
	}

	@Test
	void rejectsNullAthleteId() {
		assertThatThrownBy(
				() -> Training.create(this.periodizationId, null, this.coachId, "Treino", null, 1, 1))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsBlankName() {
		assertThatThrownBy(
				() -> Training.create(this.periodizationId, this.athleteId, this.coachId, "  ", null, 1, 1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("name must not be blank");
	}

	@Test
	void addsExerciseToTraining() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		ExercisePrescription ex = createPrescription("Supino", 0);

		t.addExercise(ex);

		assertThat(t.getExercises()).hasSize(1);
		assertThat(t.getExercises().get(0).getExerciseName()).isEqualTo("Supino");
	}

	@Test
	void removesExerciseFromTraining() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		ExercisePrescription ex = createPrescription("Supino", 0);
		t.addExercise(ex);

		t.removeExercise(ex.getId());

		assertThat(t.getExercises()).isEmpty();
	}

	@Test
	void rejectsRemovingNonExistentExercise() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);

		assertThatThrownBy(() -> t.removeExercise(UUID.randomUUID()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Exercise prescription not found");
	}

	@Test
	void reordersExercises() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		ExercisePrescription ex1 = createPrescription("Supino", 0);
		ExercisePrescription ex2 = createPrescription("Crucifixo", 1);
		ExercisePrescription ex3 = createPrescription("Crossover", 2);
		t.addExercise(ex1);
		t.addExercise(ex2);
		t.addExercise(ex3);

		t.reorderExercises(List.of(ex3.getId(), ex1.getId(), ex2.getId()));

		assertThat(t.getExercises()).extracting(ExercisePrescription::getExerciseName)
			.containsExactly("Crossover", "Supino", "Crucifixo");
		assertThat(t.getExercises()).extracting(ExercisePrescription::getOrderIndex)
			.containsExactly(0, 1, 2);
	}

	@Test
	void rejectsReorderWithWrongSize() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		t.addExercise(createPrescription("Supino", 0));

		assertThatThrownBy(() -> t.reorderExercises(List.of(UUID.randomUUID(), UUID.randomUUID())))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("same prescriptions");
	}

	@Test
	void rejectsReorderWithUnknownId() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		t.addExercise(createPrescription("Supino", 0));

		assertThatThrownBy(() -> t.reorderExercises(List.of(UUID.randomUUID())))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Prescription not found");
	}

	@Test
	void transitionsStatusDraftToInReview() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);

		t.transitionTo(TrainingStatus.IN_REVIEW);

		assertThat(t.getStatus()).isEqualTo(TrainingStatus.IN_REVIEW);
	}

	@Test
	void transitionsFullFlow() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);

		t.transitionTo(TrainingStatus.IN_REVIEW);
		t.transitionTo(TrainingStatus.ACTIVE);
		t.transitionTo(TrainingStatus.FINISHED);

		assertThat(t.getStatus()).isEqualTo(TrainingStatus.FINISHED);
	}

	@Test
	void rejectsInvalidTransitionDraftToActive() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);

		assertThatThrownBy(() -> t.transitionTo(TrainingStatus.ACTIVE))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Invalid status transition");
	}

	@Test
	void rejectsInvalidTransitionDraftToFinished() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);

		assertThatThrownBy(() -> t.transitionTo(TrainingStatus.FINISHED))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Invalid status transition");
	}

	@Test
	void rejectsTransitionFromFinished() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		t.transitionTo(TrainingStatus.IN_REVIEW);
		t.transitionTo(TrainingStatus.ACTIVE);
		t.transitionTo(TrainingStatus.FINISHED);

		assertThatThrownBy(() -> t.transitionTo(TrainingStatus.DRAFT))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Invalid status transition");
	}

	@Test
	void blocksEditWhenActive() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		t.transitionTo(TrainingStatus.IN_REVIEW);
		t.transitionTo(TrainingStatus.ACTIVE);

		assertThatThrownBy(() -> t.addExercise(createPrescription("Supino", 0)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("DRAFT or IN_REVIEW");
	}

	@Test
	void blocksEditWhenFinished() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		t.transitionTo(TrainingStatus.IN_REVIEW);
		t.transitionTo(TrainingStatus.ACTIVE);
		t.transitionTo(TrainingStatus.FINISHED);

		assertThatThrownBy(() -> t.addExercise(createPrescription("Supino", 0)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("DRAFT or IN_REVIEW");
	}

	@Test
	void allowsEditInReview() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		t.transitionTo(TrainingStatus.IN_REVIEW);

		t.addExercise(createPrescription("Supino", 0));

		assertThat(t.getExercises()).hasSize(1);
	}

	@Test
	void softDeletesTraining() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);

		t.softDelete();

		assertThat(t.isDeleted()).isTrue();
		assertThat(t.getDeletedAt()).isNotNull();
	}

	@Test
	void rejectsOperationOnSoftDeletedTraining() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);
		t.softDelete();

		assertThatThrownBy(() -> t.addExercise(createPrescription("Supino", 0)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("soft-deleted");

		assertThatThrownBy(() -> t.transitionTo(TrainingStatus.IN_REVIEW))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("soft-deleted");
	}

	@Test
	void updatesDetails() {
		Training t = Training.create(this.periodizationId, this.athleteId, this.coachId, "Treino A", null, 1, 1);

		t.updateDetails("Treino A v2", "Atualizado", 2, 3);

		assertThat(t.getName()).isEqualTo("Treino A v2");
		assertThat(t.getDescription()).isEqualTo("Atualizado");
		assertThat(t.getWeekNumber()).isEqualTo(2);
		assertThat(t.getDayOfWeek()).isEqualTo(3);
	}

	private ExercisePrescription createPrescription(String name, int orderIndex) {
		return ExercisePrescription.create(UUID.randomUUID(), name, false, orderIndex, 3, 10, new BigDecimal("60"),
				IntensityType.RPE, new BigDecimal("7.5"), 90, null);
	}

}
