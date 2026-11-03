package com.fitness.procoach.notification.interfaces;

import com.fitness.procoach.notification.application.NotificationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Aviso para o atleta (ex.: o coach ajustou um exercício do treino).")
public record NotificationResponse(
		UUID id,
		@Schema(description = "Tipo do aviso", example = "TRAINING_CHANGED") String type,
		@Schema(description = "Texto do aviso",
				example = "Seu coach ajustou \"Supino\" no treino \"Treino A\": séries 4 → 5.") String message,
		@Schema(description = "Tipo do recurso a que o aviso se refere", example = "Training") String referenceType,
		@Schema(description = "Id do recurso a que o aviso se refere") UUID referenceId,
		Instant createdAt,
		@Schema(description = "true quando o atleta já leu o aviso") boolean read,
		Instant readAt) {

	static NotificationResponse from(NotificationService.NotificationView view) {
		return new NotificationResponse(view.id(), view.type(), view.message(), view.referenceType(),
				view.referenceId(), view.createdAt(), view.read(), view.readAt());
	}
}
