package com.procoach.core.audit.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredEventJpaRepository extends JpaRepository<StoredEventJpaEntity, Long> {

	List<StoredEventJpaEntity> findByAggregateIdOrderByIdAsc(UUID aggregateId);

	List<StoredEventJpaEntity> findAllByOrderByIdAsc();
}
