package com.procoach.core.audit.domain;

import java.util.List;
import java.util.UUID;

/**
 * Porta do event store (Event Sourcing seletivo). Implementação em
 * infrastructure. Apenas append e leitura — nunca update/delete.
 */
public interface EventStore {

	StoredEvent append(StoredEvent event);

	List<StoredEvent> loadByAggregate(UUID aggregateId);

	List<StoredEvent> loadAll();
}
