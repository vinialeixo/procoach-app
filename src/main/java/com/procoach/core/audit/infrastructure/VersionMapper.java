package com.procoach.core.audit.infrastructure;

import com.procoach.core.audit.domain.Version;

final class VersionMapper {

	private VersionMapper() {
	}

	static VersionJpaEntity toEntity(Version version) {
		VersionJpaEntity entity = new VersionJpaEntity();
		entity.setId(version.getId());
		entity.setAggregateId(version.getAggregateId());
		entity.setVersionNumber(version.getVersionNumber());
		entity.setData(version.getData());
		entity.setAuthorId(version.getAuthorId());
		entity.setChangedAt(version.getChangedAt());
		entity.setReason(version.getReason());
		return entity;
	}

	static Version toDomain(VersionJpaEntity entity) {
		return Version.reconstruct(entity.getId(), entity.getAggregateId(), entity.getVersionNumber(), entity.getData(),
				entity.getAuthorId(), entity.getChangedAt(), entity.getReason());
	}
}
