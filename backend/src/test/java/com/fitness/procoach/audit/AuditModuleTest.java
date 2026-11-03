package com.fitness.procoach.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.audit.application.VersionService;
import com.fitness.procoach.audit.domain.AuditLog;
import com.fitness.procoach.audit.domain.Version;

@SpringBootTest(classes = ProcoachApplication.class)
@Import(TestcontainersConfiguration.class)
class AuditModuleTest {

	@Autowired
	private AuditService auditService;

	@Autowired
	private VersionService versionService;

	@Test
	void recordsAppendOnlyAuditLog() {
		UUID aggregateId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();

		AuditLog first = auditService.record("Diet", aggregateId, "DIET_CREATED", actorId, "{\"v\":1}");
		AuditLog second = auditService.record("Diet", aggregateId, "DIET_UPDATED", actorId, "{\"v\":2}");

		assertThat(first.getId()).isNotNull();
		assertThat(second.getId()).isNotNull();

		List<AuditLog> logs = auditService.findByAggregate(aggregateId);
		assertThat(logs).hasSize(2);
		assertThat(logs.get(0).getEventType()).isEqualTo("DIET_CREATED");
		assertThat(logs.get(1).getEventType()).isEqualTo("DIET_UPDATED");
	}

	@Test
	void createsSequentialVersionsWithoutOverwriting() {
		UUID aggregateId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();

		Version v1 = versionService.createVersion(aggregateId, "{\"cal\":2000}", authorId, "criacao");
		Version v2 = versionService.createVersion(aggregateId, "{\"cal\":2200}", authorId, "ajuste");

		assertThat(v1.getVersionNumber()).isEqualTo(1);
		assertThat(v2.getVersionNumber()).isEqualTo(2);

		List<Version> versions = versionService.findByAggregate(aggregateId);
		assertThat(versions).hasSize(2);
		assertThat(versions.get(0).getVersionNumber()).isEqualTo(1);
		assertThat(versions.get(1).getVersionNumber()).isEqualTo(2);
		assertThat(versions.get(0).getData()).isEqualTo("{\"cal\":2000}");
		assertThat(versions.get(1).getData()).isEqualTo("{\"cal\":2200}");
	}
}
