package com.fitness.procoach.audit.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoria (append-only). Uma vez persistido, nunca é alterado
 * nem removido. Registra cadastro, alteração, exclusão lógica, login, etc.
 */
public class AuditLog {

	private final UUID id;
	private final String aggregateType;
	private final UUID aggregateId;
	private final String eventType;
	private final UUID actorId;
	private final Instant occurredAt;
	private final String data;
	private final long version;

	private AuditLog(UUID id, String aggregateType, UUID aggregateId, String eventType, UUID actorId,
			Instant occurredAt, String data, long version) {
		this.id = id;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.actorId = actorId;
		this.occurredAt = occurredAt;
		this.data = data;
		this.version = version;
	}

	public static AuditLog record(String aggregateType, UUID aggregateId, String eventType, UUID actorId, String data) {
		return new AuditLog(UUID.randomUUID(), aggregateType, aggregateId, eventType, actorId, Instant.now(), data, 0);
	}

	public static AuditLog reconstruct(UUID id, String aggregateType, UUID aggregateId, String eventType, UUID actorId,
			Instant occurredAt, String data, long version) {
		return new AuditLog(id, aggregateType, aggregateId, eventType, actorId, occurredAt, data, version);
	}

	public UUID getId() {
		return id;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public UUID getAggregateId() {
		return aggregateId;
	}

	public String getEventType() {
		return eventType;
	}

	public UUID getActorId() {
		return actorId;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public String getData() {
		return data;
	}

	public long getVersion() {
		return version;
	}
}
