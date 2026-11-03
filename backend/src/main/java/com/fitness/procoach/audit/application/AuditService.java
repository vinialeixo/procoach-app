package com.fitness.procoach.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitness.procoach.audit.domain.AuditLog;
import com.fitness.procoach.audit.domain.AuditLogRepository;

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

	/** Registro ligado a um atleta; entra na transação do chamador (atomicidade com a mudança auditada). */
	@Transactional
	public AuditLog record(String aggregateType, UUID aggregateId, UUID athleteId, String eventType, UUID actorId,
			String data) {
		return auditLogRepository.append(AuditLog.record(aggregateType, aggregateId, athleteId, eventType, actorId,
				data));
	}

	/** Visão de leitura de um registro, sem expor o agregado de auditoria a outros módulos. */
	public record AuditEntry(UUID id, String aggregateType, UUID aggregateId, String eventType, UUID actorId,
			Instant occurredAt, String data) {
	}

	/** Histórico do atleta, do mais recente para o mais antigo. A posse deve ser validada pelo chamador. */
	@Transactional(readOnly = true)
	public List<AuditEntry> entriesByAthlete(UUID athleteId) {
		return auditLogRepository.findByAthlete(athleteId).stream()
				.map(log -> new AuditEntry(log.getId(), log.getAggregateType(), log.getAggregateId(),
						log.getEventType(), log.getActorId(), log.getOccurredAt(), log.getData()))
				.toList();
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
