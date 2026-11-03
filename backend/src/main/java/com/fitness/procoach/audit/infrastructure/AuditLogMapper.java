package com.fitness.procoach.audit.infrastructure;

import com.fitness.procoach.audit.domain.AuditLog;

final class AuditLogMapper {

	private AuditLogMapper() {
	}

	static AuditLogJpaEntity toEntity(AuditLog log) {
		AuditLogJpaEntity entity = new AuditLogJpaEntity();
		entity.setId(log.getId());
		entity.setAggregateType(log.getAggregateType());
		entity.setAggregateId(log.getAggregateId());
		entity.setEventType(log.getEventType());
		entity.setActorId(log.getActorId());
		entity.setOccurredAt(log.getOccurredAt());
		entity.setData(log.getData());
		entity.setVersion(log.getVersion());
		return entity;
	}

	static AuditLog toDomain(AuditLogJpaEntity entity) {
		return AuditLog.reconstruct(entity.getId(), entity.getAggregateType(), entity.getAggregateId(),
				entity.getEventType(), entity.getActorId(), entity.getOccurredAt(), entity.getData(),
				entity.getVersion());
	}
}
