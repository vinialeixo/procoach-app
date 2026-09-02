package com.procoach.core.audit.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.procoach.core.audit.domain.AuditLog;
import com.procoach.core.audit.domain.AuditLogRepository;

@Service
public class AuditService {

	private final AuditLogRepository auditLogRepository;

	public AuditService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public AuditLog record(String aggregateType, UUID aggregateId, String eventType, UUID actorId, String data) {
		return auditLogRepository.append(AuditLog.record(aggregateType, aggregateId, eventType, actorId, data));
	}

	@Transactional(readOnly = true)
	public List<AuditLog> findByAggregate(UUID aggregateId) {
		return auditLogRepository.findByAggregate(aggregateId);
	}

	@Transactional(readOnly = true)
	public List<AuditLog> findAll() {
		return auditLogRepository.findAll();
	}
}
