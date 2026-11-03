package com.fitness.procoach.security.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Convite criado. O token aparece só nesta resposta — repasse o link ao atleta.")
public record InviteResponse(UUID athleteId, String email,
		@Schema(description = "Token de uso único") String token,
		@Schema(description = "Caminho da tela de aceite no front", example = "/convite/abc123") String invitePath,
		Instant expiresAt) {
}
