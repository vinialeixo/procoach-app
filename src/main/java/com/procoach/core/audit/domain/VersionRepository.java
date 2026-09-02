package com.procoach.core.audit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VersionRepository {

	Version save(Version version);

	Optional<Version> findLatest(UUID aggregateId);

	List<Version> findByAggregate(UUID aggregateId);
}
