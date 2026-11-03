package com.fitness.procoach.audit.domain;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {

	AuditLog append(AuditLog log);

	List<AuditLog> findByAggregate(UUID aggregateId);

	/** Histórico de um atleta, do mais recente para o mais antigo. */
	List<AuditLog> findByAthlete(UUID athleteId);

	List<AuditLog> findAll();
}
