package com.procoach.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.procoach.core.audit.application.EventStoreService;
import com.procoach.core.audit.domain.StoredEvent;

@SpringBootTest(classes = ProcoachApplication.class)
@Import(TestcontainersConfiguration.class)
class EventStoreServiceTest {

	@Autowired
	private EventStoreService eventStoreService;

	@Test
	void appendsAndLoadsEventsInOrder() {
		UUID aggregateId = UUID.randomUUID();

		StoredEvent first = eventStoreService.append(
				StoredEvent.of("Athlete", aggregateId, "ATHLETE_CREATED", "{\"name\":\"Joao\"}"));
		StoredEvent second = eventStoreService.append(
				StoredEvent.of("Athlete", aggregateId, "ATHLETE_UPDATED", "{\"name\":\"Joao Silva\"}"));

		assertThat(first.getId()).isNotNull();
		assertThat(second.getId()).isNotNull();
		assertThat(second.getId()).isGreaterThan(first.getId());

		List<StoredEvent> loaded = eventStoreService.loadByAggregate(aggregateId);
		assertThat(loaded).hasSize(2);
		assertThat(loaded.get(0).getEventType()).isEqualTo("ATHLETE_CREATED");
		assertThat(loaded.get(1).getEventType()).isEqualTo("ATHLETE_UPDATED");
	}

	@Test
	void loadsAllEventsAcrossAggregates() {
		eventStoreService.append(StoredEvent.of("Athlete", UUID.randomUUID(), "ATHLETE_CREATED", "{}"));
		eventStoreService.append(StoredEvent.of("Diet", UUID.randomUUID(), "DIET_CREATED", "{}"));

		assertThat(eventStoreService.loadAll()).hasSizeGreaterThanOrEqualTo(2);
	}
}
