package com.fitness.procoach.audit.domain;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {

	AuditLog append(AuditLog log);

	List<AuditLog> findByAggregate(UUID aggregateId);

	List<AuditLog> findAll();
}
