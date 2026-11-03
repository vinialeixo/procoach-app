package com.fitness.procoach.security.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Convite para o atleta criar a própria credencial de acesso.")
public record CreateInviteRequest(
		@Schema(description = "Atleta da carteira do coach") @NotNull UUID athleteId,
		@Schema(description = "E-mail que o atleta usará para entrar", example = "ana@exemplo.com")
		@NotBlank @Email @Size(max = 255) String email) {
}
