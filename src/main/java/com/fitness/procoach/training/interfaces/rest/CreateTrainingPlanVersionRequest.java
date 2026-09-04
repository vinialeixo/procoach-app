package com.fitness.procoach.training.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados da nova versão do plano de treino. A versão de origem permanece intacta e passa a "
		+ "ser somente leitura — nunca é sobrescrita.")
public record CreateTrainingPlanVersionRequest(
		@Schema(description = "Novo nome do plano", example = "Plano de Hipertrofia - Bloco 1 (ajustado)")
		String newName,
		@Schema(description = "Nova descrição do plano",
				example = "Ajuste de volume após 4 semanas: mantém 4x/semana com progressão de carga.")
		String newDescription) {
}
