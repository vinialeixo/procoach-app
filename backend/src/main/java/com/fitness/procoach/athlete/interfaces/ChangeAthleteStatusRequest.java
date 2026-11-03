package com.fitness.procoach.athlete.interfaces;

import com.fitness.procoach.athlete.domain.AthleteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada da troca de status do atleta (US-005). O motivo é obrigatório.
 */
@Schema(description = "Troca de status do atleta, com motivo obrigatório.")
public record ChangeAthleteStatusRequest(
		@Schema(description = "Novo status", example = "PAUSED")
		@NotNull(message = "status é obrigatório") AthleteStatus status,
		@Schema(description = "Motivo da alteração", example = "Viagem de duas semanas")
		@NotBlank(message = "reason é obrigatório") @Size(max = 500, message = "reason deve ter no máximo 500 caracteres") String reason) {
}
