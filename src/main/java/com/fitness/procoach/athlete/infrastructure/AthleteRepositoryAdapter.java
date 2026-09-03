package com.fitness.procoach.athlete.infrastructure;

import com.fitness.procoach.athlete.domain.Athlete;
import com.fitness.procoach.athlete.domain.AthleteId;
import com.fitness.procoach.athlete.domain.AthleteRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter que implementa a porta {@link AthleteRepository} sobre o repositório
 * Spring Data JPA.
 */
@Repository
public class AthleteRepositoryAdapter implements AthleteRepository {

	private final AthleteJpaRepository jpaRepository;

	public AthleteRepositoryAdapter(AthleteJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Athlete save(Athlete athlete) {
		return AthleteMapper.toDomain(jpaRepository.save(AthleteMapper.toEntity(athlete)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Athlete> findById(AthleteId id) {
		return jpaRepository.findById(id.value()).map(AthleteMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsById(AthleteId id) {
		return jpaRepository.existsById(id.value());
	}
}
