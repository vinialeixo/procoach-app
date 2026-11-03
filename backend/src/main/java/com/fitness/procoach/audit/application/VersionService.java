package com.fitness.procoach.audit.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitness.procoach.audit.domain.Version;
import com.fitness.procoach.audit.domain.VersionRepository;

@Service
public class VersionService {

	private final VersionRepository versionRepository;

	public VersionService(VersionRepository versionRepository) {
		this.versionRepository = versionRepository;
	}

	@Transactional
	public Version createVersion(UUID aggregateId, String data, UUID authorId, String reason) {
		int next = versionRepository.findLatest(aggregateId)
				.map(v -> v.getVersionNumber() + 1)
				.orElse(1);
		return versionRepository.save(Version.create(aggregateId, next, data, authorId, reason));
	}

	@Transactional(readOnly = true)
	public List<Version> findByAggregate(UUID aggregateId) {
		return versionRepository.findByAggregate(aggregateId);
	}
}
