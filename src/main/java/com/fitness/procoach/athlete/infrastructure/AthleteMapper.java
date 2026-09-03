package com.fitness.procoach.athlete.infrastructure;

import com.fitness.procoach.athlete.domain.Athlete;
import com.fitness.procoach.athlete.domain.AthleteId;
import com.fitness.procoach.athlete.domain.Height;
import com.fitness.procoach.athlete.domain.Name;
import com.fitness.procoach.athlete.domain.Weight;

/**
 * Mapeamento entre o agregado de domínio e a entidade JPA.
 */
final class AthleteMapper {

	private AthleteMapper() {
	}

	static AthleteJpaEntity toEntity(Athlete athlete) {
		AthleteJpaEntity entity = new AthleteJpaEntity();
		entity.setId(athlete.id().value());
		entity.setCoachId(athlete.coachId());
		entity.setName(athlete.name().value());
		entity.setSex(athlete.sex());
		entity.setBirthDate(athlete.birthDate());
		entity.setHeight(athlete.height().value());
		entity.setWeight(athlete.weight().value());
		entity.setGoal(athlete.goal());
		entity.setStatus(athlete.status());
		entity.setCreatedAt(athlete.createdAt());
		entity.setUpdatedAt(athlete.updatedAt());
		return entity;
	}

	static Athlete toDomain(AthleteJpaEntity entity) {
		return Athlete.reconstitute(
				AthleteId.of(entity.getId()),
				entity.getCoachId(),
				new Name(entity.getName()),
				entity.getSex(),
				entity.getBirthDate(),
				new Height(entity.getHeight()),
				new Weight(entity.getWeight()),
				entity.getGoal(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
