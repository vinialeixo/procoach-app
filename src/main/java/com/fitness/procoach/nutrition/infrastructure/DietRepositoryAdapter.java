package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.nutrition.domain.AthleteId;
import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietId;
import com.fitness.procoach.nutrition.domain.DietRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter que implementa a porta {@link DietRepository} sobre o repositório
 * Spring Data JPA.
 */
@Repository
public class DietRepositoryAdapter implements DietRepository {

	private final DietJpaRepository jpaRepository;

	public DietRepositoryAdapter(DietJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Diet save(Diet diet) {
		return DietMapper.toDomain(jpaRepository.save(DietMapper.toEntity(diet)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Diet> findById(DietId id) {
		return jpaRepository.findById(id.value()).map(DietMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Diet> findAllVersionsByAthleteId(AthleteId athleteId) {
		return jpaRepository.findByAthleteIdOrderByVersionAsc(athleteId.value()).stream()
				.map(DietMapper::toDomain)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Diet> findLatestByAthleteId(AthleteId athleteId) {
		return jpaRepository.findFirstByAthleteIdOrderByVersionDesc(athleteId.value())
				.map(DietMapper::toDomain);
	}
}
