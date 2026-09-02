package com.fitness.procoach.training.interfaces.rest;

import java.util.List;
import java.util.UUID;

public record ReorderExercisesRequest(List<UUID> orderedPrescriptionIds) {
}
