package com.procoach.core.audit.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Versão de um agregado versionado (nunca sobrescrever). Registra número da
 * versão, autor, data e motivo da alteração.
 */
public class Version {

	private final UUID id;
	private final UUID aggregateId;
	private final int versionNumber;
	private final String data;
	private final UUID authorId;
	private final Instant changedAt;
	private final String reason;

	private Version(UUID id, UUID aggregateId, int versionNumber, String data, UUID authorId, Instant changedAt,
			String reason) {
		this.id = id;
		this.aggregateId = aggregateId;
		this.versionNumber = versionNumber;
		this.data = data;
		this.authorId = authorId;
		this.changedAt = changedAt;
		this.reason = reason;
	}

	public static Version create(UUID aggregateId, int versionNumber, String data, UUID authorId, String reason) {
		return new Version(UUID.randomUUID(), aggregateId, versionNumber, data, authorId, Instant.now(), reason);
	}

	public static Version reconstruct(UUID id, UUID aggregateId, int versionNumber, String data, UUID authorId,
			Instant changedAt, String reason) {
		return new Version(id, aggregateId, versionNumber, data, authorId, changedAt, reason);
	}

	public UUID getId() {
		return id;
	}

	public UUID getAggregateId() {
		return aggregateId;
	}

	public int getVersionNumber() {
		return versionNumber;
	}

	public String getData() {
		return data;
	}

	public UUID getAuthorId() {
		return authorId;
	}

	public Instant getChangedAt() {
		return changedAt;
	}

	public String getReason() {
		return reason;
	}
}
