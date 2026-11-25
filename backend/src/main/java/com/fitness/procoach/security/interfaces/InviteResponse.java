package com.fitness.procoach.security.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Convite criado. O token aparece só nesta resposta — repasse o link ao atleta se o envio por e-mail falhar.")
public record InviteResponse(UUID athleteId, String email,
		@Schema(description = "Token de uso único") String token,
		@Schema(description = "Caminho da tela de aceite no front", example = "/convite/abc123") String invitePath,
		@Schema(description = "true quando o convite foi enviado por e-mail") boolean emailSent,
		Instant expiresAt) {
}
