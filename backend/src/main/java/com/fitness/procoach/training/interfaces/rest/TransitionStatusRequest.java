package com.fitness.procoach.training.interfaces.rest;

import com.fitness.procoach.training.domain.model.TrainingStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitação de transição de status (fluxo DRAFT → IN_REVIEW → ACTIVE → FINISHED).")
public record TransitionStatusRequest(
		@Schema(description = "Status de destino da transição", example = "IN_REVIEW")
		@NotNull(message = "targetStatus é obrigatório") TrainingStatus targetStatus) {
}
