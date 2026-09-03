package com.fitness.procoach.audit.infrastructure;

import com.fitness.procoach.audit.domain.StoredEvent;

final class StoredEventMapper {

	private StoredEventMapper() {
	}

	static StoredEventJpaEntity toEntity(StoredEvent event) {
		StoredEventJpaEntity entity = new StoredEventJpaEntity();
		entity.setId(event.getId());
		entity.setAggregateType(event.getAggregateType());
		entity.setAggregateId(event.getAggregateId());
		entity.setEventType(event.getEventType());
		entity.setEventData(event.getEventData());
		entity.setOccurredAt(event.getOccurredAt());
		return entity;
	}

	static StoredEvent toDomain(StoredEventJpaEntity entity) {
		return StoredEvent.reconstruct(entity.getId(), entity.getAggregateType(), entity.getAggregateId(),
				entity.getEventType(), entity.getEventData(), entity.getOccurredAt());
	}
}
