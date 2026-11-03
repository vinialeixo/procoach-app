package com.fitness.procoach.notification.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Canais externos que o atleta aceita para receber avisos. O aviso dentro do app sempre existe.")
public record NotificationPreferencesRequest(
		@Schema(description = "Receber avisos por e-mail", example = "true")
		@NotNull(message = "emailEnabled é obrigatório") Boolean emailEnabled,
		@Schema(description = "Receber avisos por push", example = "true")
		@NotNull(message = "pushEnabled é obrigatório") Boolean pushEnabled) {
}
