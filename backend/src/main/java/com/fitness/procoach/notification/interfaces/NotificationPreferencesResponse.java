package com.fitness.procoach.notification.interfaces;

import com.fitness.procoach.notification.application.NotificationSettingsService;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preferências de canal de aviso do atleta (padrão: tudo ligado).")
public record NotificationPreferencesResponse(boolean emailEnabled, boolean pushEnabled) {

	static NotificationPreferencesResponse from(NotificationSettingsService.Preferences preferences) {
		return new NotificationPreferencesResponse(preferences.emailEnabled(), preferences.pushEnabled());
	}
}
