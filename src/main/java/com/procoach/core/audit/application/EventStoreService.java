package com.procoach.core.audit.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.procoach.core.audit.domain.EventStore;
import com.procoach.core.audit.domain.StoredEvent;

@Service
public class EventStoreService {

	private final EventStore eventStore;

	public EventStoreService(EventStore eventStore) {
		this.eventStore = eventStore;
	}

	@Transactional
	public StoredEvent append(StoredEvent event) {
		return eventStore.append(event);
	}

	@Transactional(readOnly = true)
	public List<StoredEvent> loadByAggregate(UUID aggregateId) {
		return eventStore.loadByAggregate(aggregateId);
	}

	@Transactional(readOnly = true)
	public List<StoredEvent> loadAll() {
		return eventStore.loadAll();
	}
}
