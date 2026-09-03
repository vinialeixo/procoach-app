package com.fitness.procoach.audit.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

	List<AuditLogJpaEntity> findByAggregateIdOrderByOccurredAtAsc(UUID aggregateId);

	List<AuditLogJpaEntity> findAllByOrderByOccurredAtAsc();
}
