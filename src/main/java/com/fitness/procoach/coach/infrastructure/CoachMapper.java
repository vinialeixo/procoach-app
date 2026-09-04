package com.fitness.procoach.coach.infrastructure;

import com.fitness.procoach.coach.domain.Coach;
import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.coach.domain.CoachStatus;

/**
 * Mapeamento entre o agregado de domínio Coach e a entidade JPA.
 */
final class CoachMapper {

	private CoachMapper() {
	}

	static CoachJpaEntity toEntity(Coach coach) {
		CoachJpaEntity entity = new CoachJpaEntity();
		entity.setId(coach.id().value());
		entity.setUserId(coach.userId());
		entity.setName(coach.name());
		entity.setStatus(coach.status());
		entity.setCreatedAt(coach.createdAt());
		entity.setUpdatedAt(coach.updatedAt());
		return entity;
	}

	static Coach toDomain(CoachJpaEntity entity) {
		return Coach.reconstitute(
				CoachId.of(entity.getId()),
				entity.getUserId(),
				entity.getName(),
				entity.getStatus() != null ? entity.getStatus() : CoachStatus.ACTIVE,
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
