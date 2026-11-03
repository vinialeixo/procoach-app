package com.fitness.procoach.security.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Senha escolhida pelo atleta ao aceitar o convite.")
public record AcceptInviteRequest(
		@Schema(description = "Senha (8 a 72 caracteres)") @NotBlank @Size(min = 8, max = 72) String password) {
}
