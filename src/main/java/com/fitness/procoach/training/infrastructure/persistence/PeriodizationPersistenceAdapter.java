package com.fitness.procoach.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.repository.PeriodizationRepository;

import org.springframework.stereotype.Component;

@Component
public class PeriodizationPersistenceAdapter implements PeriodizationRepository {

	private final PeriodizationSpringDataRepository springDataRepository;

	public PeriodizationPersistenceAdapter(PeriodizationSpringDataRepository springDataRepository) {
		this.springDataRepository = springDataRepository;
	}

	@Override
	public Periodization save(Periodization periodization) {
		PeriodizationJpaEntity entity = toEntity(periodization);
		PeriodizationJpaEntity saved = this.springDataRepository.save(entity);
		return toDomain(saved);
	}

	@Override
	public Optional<Periodization> findById(UUID id) {
		return this.springDataRepository.findById(id).map(this::toDomain);
	}

	@Override
	public List<Periodization> findByAthleteId(UUID athleteId) {
		return this.springDataRepository.findByAthleteId(athleteId).stream().map(this::toDomain).toList();
	}

	@Override
	public List<Periodization> findByCoachId(UUID coachId) {
		return this.springDataRepository.findByCoachId(coachId).stream().map(this::toDomain).toList();
	}

	@Override
	public List<Periodization> findByAthleteIdAndDeletedFalse(UUID athleteId) {
		return this.springDataRepository.findByAthleteIdAndDeletedFalse(athleteId).stream().map(this::toDomain).toList();
	}

	private PeriodizationJpaEntity toEntity(Periodization domain) {
		PeriodizationJpaEntity entity = new PeriodizationJpaEntity();
		entity.setId(domain.getId());
		entity.setAthleteId(domain.getAthleteId());
		entity.setCoachId(domain.getCoachId());
		entity.setName(domain.getName());
		entity.setDescription(domain.getDescription());
		entity.setStartDate(domain.getStartDate());
		entity.setEndDate(domain.getEndDate());
		entity.setStatus(domain.getStatus());
		entity.setCreatedAt(domain.getCreatedAt());
		entity.setUpdatedAt(domain.getUpdatedAt());
		entity.setDeleted(domain.isDeleted());
		entity.setDeletedAt(domain.getDeletedAt());
		return entity;
	}

	private Periodization toDomain(PeriodizationJpaEntity entity) {
		return new Periodization(entity.getId(), entity.getAthleteId(), entity.getCoachId(), entity.getName(),
				entity.getDescription(), entity.getStartDate(), entity.getEndDate(), entity.getStatus(),
				entity.getCreatedAt(), entity.getUpdatedAt(), entity.isDeleted(), entity.getDeletedAt());
	}

}
