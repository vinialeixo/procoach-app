package com.fitness.procoach.audit.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fitness.procoach.audit.domain.EventStore;
import com.fitness.procoach.audit.domain.StoredEvent;

@Repository
public class JpaEventStore implements EventStore {

	private final StoredEventJpaRepository repository;

	public JpaEventStore(StoredEventJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public StoredEvent append(StoredEvent event) {
		StoredEventJpaEntity saved = repository.save(StoredEventMapper.toEntity(event));
		return StoredEventMapper.toDomain(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredEvent> loadByAggregate(UUID aggregateId) {
		return repository.findByAggregateIdOrderByIdAsc(aggregateId).stream()
				.map(StoredEventMapper::toDomain)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredEvent> loadAll() {
		return repository.findAllByOrderByIdAsc().stream()
				.map(StoredEventMapper::toDomain)
				.toList();
	}
}
