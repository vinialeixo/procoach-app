package com.fitness.procoach.notification.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aviso in-app para o atleta (ex.: o coach ajustou um exercício do treino ativo). Nasce
 * não lido; {@link #markRead(Instant)} é idempotente.
 */
public final class Notification {

	/** Tamanho máximo da mensagem (coluna VARCHAR(500)). */
	public static final int MAX_MESSAGE_LENGTH = 500;

	private final UUID id;
	private final UUID athleteId;
	private final String type;
	private final String message;
	private final String referenceType;
	private final UUID referenceId;
	private final Instant createdAt;
	private Instant readAt;

	private Notification(UUID id, UUID athleteId, String type, String message, String referenceType,
			UUID referenceId, Instant createdAt, Instant readAt) {
		this.id = Objects.requireNonNull(id, "id");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId");
		this.type = requireText(type, "type");
		this.message = requireText(message, "message");
		this.referenceType = referenceType;
		this.referenceId = referenceId;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
		this.readAt = readAt;
	}

	public static Notification create(UUID athleteId, String type, String message, String referenceType,
			UUID referenceId, Instant now) {
		String text = requireText(message, "message");
		String bounded = text.length() > MAX_MESSAGE_LENGTH ? text.substring(0, MAX_MESSAGE_LENGTH - 1) + "…" : text;
		return new Notification(UUID.randomUUID(), athleteId, type, bounded, referenceType, referenceId, now, null);
	}

	public static Notification reconstitute(UUID id, UUID athleteId, String type, String message,
			String referenceType, UUID referenceId, Instant createdAt, Instant readAt) {
		return new Notification(id, athleteId, type, message, referenceType, referenceId, createdAt, readAt);
	}

	public boolean isRead() {
		return readAt != null;
	}

	/** Marca como lido; se já estava lido, mantém a data original. */
	public void markRead(Instant now) {
		if (readAt == null) {
			readAt = Objects.requireNonNull(now, "now");
		}
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAthleteId() {
		return athleteId;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public String getReferenceType() {
		return referenceType;
	}

	public UUID getReferenceId() {
		return referenceId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getReadAt() {
		return readAt;
	}
}
