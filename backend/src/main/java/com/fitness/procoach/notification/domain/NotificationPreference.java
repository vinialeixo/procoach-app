package com.fitness.procoach.notification.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Preferência de aviso do atleta: quais canais externos ele aceita receber. O aviso dentro do app
 * sempre existe; só e-mail e push podem ser desligados. Sem registro, vale o padrão (tudo ligado).
 */
public final class NotificationPreference {

	private final UUID athleteId;
	private final boolean emailEnabled;
	private final boolean pushEnabled;
	private final Instant updatedAt;

	private NotificationPreference(UUID athleteId, boolean emailEnabled, boolean pushEnabled, Instant updatedAt) {
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId");
		this.emailEnabled = emailEnabled;
		this.pushEnabled = pushEnabled;
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
	}

	/** Padrão de quem nunca mexeu nas preferências: todos os canais ligados. */
	public static NotificationPreference defaults(UUID athleteId) {
		return new NotificationPreference(athleteId, true, true, Instant.EPOCH);
	}

	public static NotificationPreference of(UUID athleteId, boolean emailEnabled, boolean pushEnabled, Instant now) {
		return new NotificationPreference(athleteId, emailEnabled, pushEnabled, now);
	}

	public boolean allows(NotificationChannelType channel) {
		return switch (channel) {
			case EMAIL -> emailEnabled;
			case PUSH -> pushEnabled;
		};
	}

	public UUID getAthleteId() {
		return athleteId;
	}

	public boolean isEmailEnabled() {
		return emailEnabled;
	}

	public boolean isPushEnabled() {
		return pushEnabled;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
