package com.fitness.procoach.notification.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Dispositivo do atleta registrado para receber push. O token é único: ao registrar de novo
 * (inclusive em outra conta, no mesmo aparelho) o registro muda de dono em vez de duplicar.
 */
public final class DeviceToken {

	private final UUID id;
	private UUID athleteId;
	private final String token;
	private DevicePlatform platform;
	private final Instant createdAt;
	private Instant lastSeenAt;

	private DeviceToken(UUID id, UUID athleteId, String token, DevicePlatform platform, Instant createdAt,
			Instant lastSeenAt) {
		this.id = Objects.requireNonNull(id, "id");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId");
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("token must not be blank");
		}
		this.token = token;
		this.platform = Objects.requireNonNull(platform, "platform");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
		this.lastSeenAt = Objects.requireNonNull(lastSeenAt, "lastSeenAt");
	}

	public static DeviceToken register(UUID athleteId, String token, DevicePlatform platform, Instant now) {
		return new DeviceToken(UUID.randomUUID(), athleteId, token, platform, now, now);
	}

	public static DeviceToken reconstitute(UUID id, UUID athleteId, String token, DevicePlatform platform,
			Instant createdAt, Instant lastSeenAt) {
		return new DeviceToken(id, athleteId, token, platform, createdAt, lastSeenAt);
	}

	/** Registro repetido: atualiza dono, plataforma e último uso. */
	public void seenBy(UUID athleteId, DevicePlatform platform, Instant now) {
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId");
		this.platform = Objects.requireNonNull(platform, "platform");
		this.lastSeenAt = Objects.requireNonNull(now, "now");
	}

	public UUID getId() {
		return id;
	}

	public UUID getAthleteId() {
		return athleteId;
	}

	public String getToken() {
		return token;
	}

	public DevicePlatform getPlatform() {
		return platform;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}
}
