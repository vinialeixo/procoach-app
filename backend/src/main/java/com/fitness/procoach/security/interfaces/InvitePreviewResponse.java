package com.fitness.procoach.security.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Dados do convite exibidos ao atleta antes de definir a senha.")
public record InvitePreviewResponse(String athleteName, String email, Instant expiresAt) {
}
