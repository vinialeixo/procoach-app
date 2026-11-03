package com.fitness.procoach.training.domain.model;

public enum TrainingStatus {

	DRAFT,
	IN_REVIEW,
	ACTIVE,
	FINISHED;

	public boolean canTransitionTo(TrainingStatus target) {
		return switch (this) {
			case DRAFT -> target == IN_REVIEW;
			case IN_REVIEW -> target == ACTIVE || target == DRAFT;
			case ACTIVE -> target == FINISHED;
			case FINISHED -> false;
		};
	}

}
