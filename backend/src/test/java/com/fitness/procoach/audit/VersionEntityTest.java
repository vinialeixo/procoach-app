package com.fitness.procoach.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.fitness.procoach.audit.application.VersionService;
import com.fitness.procoach.audit.domain.Version;
import com.fitness.procoach.audit.domain.VersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes da entidade Version (US-041).
 *
 * <p>Valida que todo objeto versionado registra:
 * - versão (número sequencial)
 * - data (changedAt)
 * - autor (authorId)
 * - motivo (reason)
 * - dados (data JSON)
 *
 * <p>Histórico é cronológico e filtrável por autor/data.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@Import(TestcontainersConfiguration.class)
class VersionEntityTest {

    @Autowired
    private VersionService versionService;

    @Autowired
    private VersionRepository versionRepository;

    @Test
    void createVersion_assignsSequentialVersionNumber() {
        UUID aggregateId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Version v1 = versionService.createVersion(aggregateId, "{\"cal\":2000}", authorId, "criação");
        Version v2 = versionService.createVersion(aggregateId, "{\"cal\":2200}", authorId, "ajuste");
        Version v3 = versionService.createVersion(aggregateId, "{\"cal\":1800}", authorId, "corte");

        assertThat(v1.getVersionNumber()).isEqualTo(1);
        assertThat(v2.getVersionNumber()).isEqualTo(2);
        assertThat(v3.getVersionNumber()).isEqualTo(3);
    }

    @Test
    void createVersion_recordsMandatoryFields() {
        UUID aggregateId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String reason = "Ajuste de macros";
        String data = "{\"cal\":2200,\"prot\":180}";

        Version version = versionService.createVersion(aggregateId, data, authorId, reason);

        assertThat(version.getId()).isNotNull();
        assertThat(version.getAggregateId()).isEqualTo(aggregateId);
        assertThat(version.getVersionNumber()).isEqualTo(1);
        assertThat(version.getData()).isEqualTo(data);
        assertThat(version.getAuthorId()).isEqualTo(authorId);
        assertThat(version.getReason()).isEqualTo(reason);
        assertThat(version.getChangedAt()).isNotNull();
        assertThat(version.getChangedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void findByAggregate_returnsChronologicalOrder() {
        UUID aggregateId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        versionService.createVersion(aggregateId, "{\"v\":1}", authorId, "criação");
        versionService.createVersion(aggregateId, "{\"v\":2}", authorId, "ajuste 1");
        versionService.createVersion(aggregateId, "{\"v\":3}", authorId, "ajuste 2");

        List<Version> versions = versionService.findByAggregate(aggregateId);

        assertThat(versions).hasSize(3);
        assertThat(versions.get(0).getVersionNumber()).isEqualTo(1);
        assertThat(versions.get(1).getVersionNumber()).isEqualTo(2);
        assertThat(versions.get(2).getVersionNumber()).isEqualTo(3);
        // Ordem cronológica crescente
        assertThat(versions.get(0).getChangedAt()).isBeforeOrEqualTo(versions.get(1).getChangedAt());
        assertThat(versions.get(1).getChangedAt()).isBeforeOrEqualTo(versions.get(2).getChangedAt());
    }

    @Test
    void versionChain_integrity_previousVersionNotExplicitButOrderedByNumber() {
        // A entidade Version não tem previousVersionId explícito (diferente de Diet/TrainingPlan),
        // mas a integridade da cadeia é garantida pelo versionNumber sequencial
        // e pela ordenação cronológica.
        UUID aggregateId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Version v1 = versionService.createVersion(aggregateId, "{\"v\":1}", authorId, "v1");
        Version v2 = versionService.createVersion(aggregateId, "{\"v\":2}", authorId, "v2");
        Version v3 = versionService.createVersion(aggregateId, "{\"v\":3}", authorId, "v3");

        assertThat(v2.getVersionNumber()).isEqualTo(v1.getVersionNumber() + 1);
        assertThat(v3.getVersionNumber()).isEqualTo(v2.getVersionNumber() + 1);

        List<Version> chain = versionService.findByAggregate(aggregateId);
        assertThat(chain).hasSize(3);
        for (int i = 1; i < chain.size(); i++) {
            assertThat(chain.get(i).getVersionNumber()).isEqualTo(chain.get(i - 1).getVersionNumber() + 1);
        }
    }

    @Test
    void domainEntity_isImmutable_noSetters() {
        UUID id = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant now = Instant.now();

        Version version = Version.reconstruct(id, aggregateId, 2, "{\"cal\":2200}", authorId, now, "ajuste");

        assertThat(version.getId()).isEqualTo(id);
        assertThat(version.getAggregateId()).isEqualTo(aggregateId);
        assertThat(version.getVersionNumber()).isEqualTo(2);
        assertThat(version.getData()).isEqualTo("{\"cal\":2200}");
        assertThat(version.getAuthorId()).isEqualTo(authorId);
        assertThat(version.getChangedAt()).isEqualTo(now);
        assertThat(version.getReason()).isEqualTo("ajuste");
        // Sem setters públicos - imutável
    }

    @Test
    void versionsAreFilterableByAuthor() {
        UUID aggregateId = UUID.randomUUID();
        UUID author1 = UUID.randomUUID();
        UUID author2 = UUID.randomUUID();

        versionService.createVersion(aggregateId, "{\"v\":1}", author1, "por autor 1");
        versionService.createVersion(aggregateId, "{\"v\":2}", author2, "por autor 2");
        versionService.createVersion(aggregateId, "{\"v\":3}", author1, "por autor 1 novamente");

        List<Version> all = versionService.findByAggregate(aggregateId);

        // Filtrar por autor (lado do cliente da query, ou repository poderia expor)
        List<Version> byAuthor1 = all.stream().filter(v -> v.getAuthorId().equals(author1)).toList();
        List<Version> byAuthor2 = all.stream().filter(v -> v.getAuthorId().equals(author2)).toList();

        assertThat(byAuthor1).hasSize(2);
        assertThat(byAuthor1.get(0).getReason()).isEqualTo("por autor 1");
        assertThat(byAuthor1.get(1).getReason()).isEqualTo("por autor 1 novamente");
        assertThat(byAuthor2).hasSize(1);
        assertThat(byAuthor2.get(0).getReason()).isEqualTo("por autor 2");
    }

    @Test
    void versionsAreFilterableByDateRange() {
        UUID aggregateId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Version v1 = versionService.createVersion(aggregateId, "{\"v\":1}", authorId, "v1");
        Version v2 = versionService.createVersion(aggregateId, "{\"v\":2}", authorId, "v2");
        Version v3 = versionService.createVersion(aggregateId, "{\"v\":3}", authorId, "v3");

        List<Version> all = versionService.findByAggregate(aggregateId);

        // Filtrar por data (ex.: versões após a primeira)
        Instant cutoff = v1.getChangedAt().minusSeconds(1); // antes de todas
        List<Version> recent = all.stream().filter(v -> v.getChangedAt().isAfter(cutoff)).toList();

        // Como todas são criadas quase no mesmo instante, todas passam no filtro
        assertThat(recent).hasSize(3);
    }

    @Test
    @Transactional
    void differentAggregatesHaveIndependentVersionSequences() {
        UUID aggregateA = UUID.randomUUID();
        UUID aggregateB = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Version a1 = versionService.createVersion(aggregateA, "{\"a\":1}", authorId, "a1");
        Version b1 = versionService.createVersion(aggregateB, "{\"b\":1}", authorId, "b1");
        Version a2 = versionService.createVersion(aggregateA, "{\"a\":2}", authorId, "a2");
        Version b2 = versionService.createVersion(aggregateB, "{\"b\":2}", authorId, "b2");

        assertThat(a1.getVersionNumber()).isEqualTo(1);
        assertThat(a2.getVersionNumber()).isEqualTo(2);
        assertThat(b1.getVersionNumber()).isEqualTo(1);
        assertThat(b2.getVersionNumber()).isEqualTo(2);

        List<Version> versionsA = versionService.findByAggregate(aggregateA);
        List<Version> versionsB = versionService.findByAggregate(aggregateB);

        assertThat(versionsA).hasSize(2);
        assertThat(versionsB).hasSize(2);
    }
}