package com.fitness.procoach.training.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingStatusTest {

	@Test
	void draftCanTransitionToInReview() {
		assertThat(TrainingStatus.DRAFT.canTransitionTo(TrainingStatus.IN_REVIEW)).isTrue();
	}

	@Test
	void inReviewCanTransitionToActive() {
		assertThat(TrainingStatus.IN_REVIEW.canTransitionTo(TrainingStatus.ACTIVE)).isTrue();
	}

	@Test
	void inReviewCanTransitionBackToDraft() {
		assertThat(TrainingStatus.IN_REVIEW.canTransitionTo(TrainingStatus.DRAFT)).isTrue();
	}

	@Test
	void activeCanTransitionToFinished() {
		assertThat(TrainingStatus.ACTIVE.canTransitionTo(TrainingStatus.FINISHED)).isTrue();
	}

	@ParameterizedTest
	@EnumSource(TrainingStatus.class)
	void finishedCannotTransitionToAny(TrainingStatus target) {
		assertThat(TrainingStatus.FINISHED.canTransitionTo(target)).isFalse();
	}

	@Test
	void draftCannotTransitionToActive() {
		assertThat(TrainingStatus.DRAFT.canTransitionTo(TrainingStatus.ACTIVE)).isFalse();
	}

	@Test
	void draftCannotTransitionToFinished() {
		assertThat(TrainingStatus.DRAFT.canTransitionTo(TrainingStatus.FINISHED)).isFalse();
	}

	@Test
	void activeCannotTransitionToDraft() {
		assertThat(TrainingStatus.ACTIVE.canTransitionTo(TrainingStatus.DRAFT)).isFalse();
	}

	@Test
	void activeCannotTransitionToInReview() {
		assertThat(TrainingStatus.ACTIVE.canTransitionTo(TrainingStatus.IN_REVIEW)).isFalse();
	}

}
