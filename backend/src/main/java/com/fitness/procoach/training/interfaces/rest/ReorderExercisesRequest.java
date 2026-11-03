package com.fitness.procoach.training.interfaces.rest;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lista ordenada de ids de prescrição que redefine a ordem dos exercícios do treino.")
public record ReorderExercisesRequest(
		@Schema(description = "Ids das prescrições na ordem desejada de execução",
				example = "[\"3a4b5c6d-7e8f-4a9b-0c1d-2e3f4a5b6c7d\", \"5d4c3b2a-1f0e-4d5c-8b7a-6f5e4d3c2b1a\"]")
		List<UUID> orderedPrescriptionIds) {
}
