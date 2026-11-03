package com.fitness.procoach.coach.infrastructure;

import com.fitness.procoach.coach.domain.Coach;
import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.coach.domain.CoachRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter que implementa a porta {@link CoachRepository} sobre o repositório
 * Spring Data JPA.
 */
@Repository
public class CoachRepositoryAdapter implements CoachRepository {

	private final CoachJpaRepository jpaRepository;

	public CoachRepositoryAdapter(CoachJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Coach save(Coach coach) {
		return CoachMapper.toDomain(jpaRepository.save(CoachMapper.toEntity(coach)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Coach> findById(CoachId id) {
		return jpaRepository.findById(id.value()).map(CoachMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Coach> findByUserId(UUID userId) {
		return jpaRepository.findByUserId(userId).map(CoachMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByUserId(UUID userId) {
		return jpaRepository.existsByUserId(userId);
	}
}
