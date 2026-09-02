package com.procoach.core.audit.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionJpaRepository extends JpaRepository<VersionJpaEntity, UUID> {

	Optional<VersionJpaEntity> findFirstByAggregateIdOrderByVersionNumberDesc(UUID aggregateId);

	List<VersionJpaEntity> findByAggregateIdOrderByVersionNumberAsc(UUID aggregateId);
}
