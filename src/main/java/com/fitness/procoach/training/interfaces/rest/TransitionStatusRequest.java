package com.fitness.procoach.training.interfaces.rest;

import com.fitness.procoach.training.domain.model.TrainingStatus;

public record TransitionStatusRequest(TrainingStatus targetStatus) {
}
