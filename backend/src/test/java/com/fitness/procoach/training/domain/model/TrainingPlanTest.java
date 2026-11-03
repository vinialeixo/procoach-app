package com.fitness.procoach.training.domain.model;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingPlanTest {

	private UUID athleteId;

	private UUID coachId;

	private UUID periodizationId;

	@BeforeEach
	void setUp() {
		this.athleteId = UUID.randomUUID();
		this.coachId = UUID.randomUUID();
		this.periodizationId = UUID.randomUUID();
	}

	@Test
	void createsTrainingPlanWithVersion1() {
		TrainingPlan tp = TrainingPlan.create(this.athleteId, this.coachId, this.periodizationId, "Plano Base",
				"Hipertrofia");

		assertThat(tp.getId()).isNotNull();
		assertThat(tp.getVersion()).isEqualTo(1);
		assertThat(tp.getPreviousVersionId()).isNull();
		assertThat(tp.getStatus()).isEqualTo(TrainingStatus.DRAFT);
		assertThat(tp.getAthleteId()).isEqualTo(this.athleteId);
		assertThat(tp.getCoachId()).isEqualTo(this.coachId);
		assertThat(tp.getPeriodizationId()).isEqualTo(this.periodizationId);
	}

	@Test
	void rejectsNullPeriodizationId() {
		assertThatThrownBy(() -> TrainingPlan.create(this.athleteId, this.coachId, null, "Plano", null))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("periodizationId must not be null");
	}

	@Test
	void createsNewVersionIncrementingNumber() {
		TrainingPlan v1 = TrainingPlan.create(this.athleteId, this.coachId, this.periodizationId, "Plano v1", null);

		TrainingPlan v2 = v1.createNewVersion("Plano v2", "Ajustado");

		assertThat(v2.getVersion()).isEqualTo(2);
		assertThat(v2.getPreviousVersionId()).isEqualTo(v1.getId());
		assertThat(v2.getId()).isNotEqualTo(v1.getId());
		assertThat(v2.getStatus()).isEqualTo(TrainingStatus.DRAFT);
	}

	@Test
	void createsChainedVersions() {
		TrainingPlan v1 = TrainingPlan.create(this.athleteId, this.coachId, this.periodizationId, "Plano v1", null);
		TrainingPlan v2 = v1.createNewVersion("Plano v2", null);
		TrainingPlan v3 = v2.createNewVersion("Plano v3", null);

		assertThat(v3.getVersion()).isEqualTo(3);
		assertThat(v3.getPreviousVersionId()).isEqualTo(v2.getId());
		assertThat(v2.getPreviousVersionId()).isEqualTo(v1.getId());
		assertThat(v1.getPreviousVersionId()).isNull();
	}

	@Test
	void originalIsNotModifiedWhenCreatingNewVersion() {
		TrainingPlan v1 = TrainingPlan.create(this.athleteId, this.coachId, this.periodizationId, "Plano v1", null);
		String originalName = v1.getName();
		int originalVersion = v1.getVersion();

		v1.createNewVersion("Plano v2", null);

		assertThat(v1.getName()).isEqualTo(originalName);
		assertThat(v1.getVersion()).isEqualTo(originalVersion);
	}

	@Test
	void transitionsStatusThroughFlow() {
		TrainingPlan tp = TrainingPlan.create(this.athleteId, this.coachId, this.periodizationId, "Plano", null);

		tp.transitionTo(TrainingStatus.IN_REVIEW);
		assertThat(tp.getStatus()).isEqualTo(TrainingStatus.IN_REVIEW);

		tp.transitionTo(TrainingStatus.ACTIVE);
		assertThat(tp.getStatus()).isEqualTo(TrainingStatus.ACTIVE);

		tp.transitionTo(TrainingStatus.FINISHED);
		assertThat(tp.getStatus()).isEqualTo(TrainingStatus.FINISHED);
	}

	@Test
	void rejectsInvalidTransition() {
		TrainingPlan tp = TrainingPlan.create(this.athleteId, this.coachId, this.periodizationId, "Plano", null);

		assertThatThrownBy(() -> tp.transitionTo(TrainingStatus.FINISHED))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Invalid status transition");
	}

	@Test
	void softDeletesPlan() {
		TrainingPlan tp = TrainingPlan.create(this.athleteId, this.coachId, this.periodizationId, "Plano", null);

		tp.softDelete();

		assertThat(tp.isDeleted()).isTrue();
		assertThat(tp.getDeletedAt()).isNotNull();
	}

	@Test
	void rejectsOperationOnSoftDeletedPlan() {
		TrainingPlan tp = TrainingPlan.create(this.athleteId, this.coachId, this.periodizationId, "Plano", null);
		tp.softDelete();

		assertThatThrownBy(() -> tp.createNewVersion("v2", null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("soft-deleted");

		assertThatThrownBy(() -> tp.transitionTo(TrainingStatus.IN_REVIEW))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("soft-deleted");
	}

	@Test
	void blocksUpdateWhenActive() {
		TrainingPlan tp = TrainingPlan.create(this.athleteId, this.coachId, this.periodizationId, "Plano", null);
		tp.transitionTo(TrainingStatus.IN_REVIEW);
		tp.transitionTo(TrainingStatus.ACTIVE);

		assertThatThrownBy(() -> tp.updateDetails("Novo nome", null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("DRAFT or IN_REVIEW");
	}

}
