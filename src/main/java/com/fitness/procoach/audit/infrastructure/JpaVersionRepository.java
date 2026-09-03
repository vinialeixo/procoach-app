package com.fitness.procoach.audit.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fitness.procoach.audit.domain.Version;
import com.fitness.procoach.audit.domain.VersionRepository;

@Repository
public class JpaVersionRepository implements VersionRepository {

	private final VersionJpaRepository jpaRepository;

	public JpaVersionRepository(VersionJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Version save(Version version) {
		return VersionMapper.toDomain(jpaRepository.save(VersionMapper.toEntity(version)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Version> findLatest(UUID aggregateId) {
		return jpaRepository.findFirstByAggregateIdOrderByVersionNumberDesc(aggregateId).map(VersionMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Version> findByAggregate(UUID aggregateId) {
		return jpaRepository.findByAggregateIdOrderByVersionNumberAsc(aggregateId).stream()
				.map(VersionMapper::toDomain)
				.toList();
	}
}
