package com.fitness.procoach.audit.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fitness.procoach.audit.domain.AuditLog;
import com.fitness.procoach.audit.domain.AuditLogRepository;

@Repository
public class JpaAuditLogRepository implements AuditLogRepository {

	private final AuditLogJpaRepository jpaRepository;

	public JpaAuditLogRepository(AuditLogJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public AuditLog append(AuditLog log) {
		return AuditLogMapper.toDomain(jpaRepository.save(AuditLogMapper.toEntity(log)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AuditLog> findByAggregate(UUID aggregateId) {
		return jpaRepository.findByAggregateIdOrderByOccurredAtAsc(aggregateId).stream()
				.map(AuditLogMapper::toDomain)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AuditLog> findByAthlete(UUID athleteId) {
		return jpaRepository.findByAthleteIdOrderByOccurredAtDesc(athleteId).stream()
				.map(AuditLogMapper::toDomain)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AuditLog> findAll() {
		return jpaRepository.findAllByOrderByOccurredAtAsc().stream()
				.map(AuditLogMapper::toDomain)
				.toList();
	}
}
