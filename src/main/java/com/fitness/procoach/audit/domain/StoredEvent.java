package com.fitness.procoach.audit.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento armazenado no event store (Event Sourcing seletivo).
 * Append-only: uma vez persistido, nunca é alterado nem removido.
 */
public class StoredEvent {

	private final Long id;
	private final String aggregateType;
	private final UUID aggregateId;
	private final String eventType;
	private final String eventData;
	private final Instant occurredAt;

	private StoredEvent(Long id, String aggregateType, UUID aggregateId, String eventType, String eventData,
			Instant occurredAt) {
		this.id = id;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.eventData = eventData;
		this.occurredAt = occurredAt;
	}

	public static StoredEvent of(String aggregateType, UUID aggregateId, String eventType, String eventData) {
		return new StoredEvent(null, aggregateType, aggregateId, eventType, eventData, Instant.now());
	}

	public static StoredEvent reconstruct(Long id, String aggregateType, UUID aggregateId, String eventType,
			String eventData, Instant occurredAt) {
		return new StoredEvent(id, aggregateType, aggregateId, eventType, eventData, occurredAt);
	}

	public Long getId() {
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

	public String getEventData() {
		return eventData;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}
}
