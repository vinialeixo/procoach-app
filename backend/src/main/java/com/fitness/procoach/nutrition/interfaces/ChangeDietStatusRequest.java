package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.DietStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada da transição de status da dieta (US-017).
 */
@Schema(description = "Próximo estado da dieta no fluxo Rascunho → Em Revisão → Aprovada → Arquivada.")
public record ChangeDietStatusRequest(
		@Schema(description = "Novo status", example = "IN_REVIEW")
		@NotNull(message = "status é obrigatório") DietStatus status) {
}
