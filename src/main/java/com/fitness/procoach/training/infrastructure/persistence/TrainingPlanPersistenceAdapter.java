package com.fitness.procoach.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.model.TrainingStatus;
import com.fitness.procoach.training.domain.repository.TrainingPlanRepository;

import org.springframework.stereotype.Component;

@Component
public class TrainingPlanPersistenceAdapter implements TrainingPlanRepository {

	private final TrainingPlanSpringDataRepository springDataRepository;

	public TrainingPlanPersistenceAdapter(TrainingPlanSpringDataRepository springDataRepository) {
		this.springDataRepository = springDataRepository;
	}

	@Override
	public TrainingPlan save(TrainingPlan trainingPlan) {
		TrainingPlanJpaEntity entity = toEntity(trainingPlan);
		TrainingPlanJpaEntity saved = this.springDataRepository.save(entity);
		return toDomain(saved);
	}

	@Override
	public Optional<TrainingPlan> findById(UUID id) {
		return this.springDataRepository.findById(id).map(this::toDomain);
	}

	@Override
	public List<TrainingPlan> findByAthleteId(UUID athleteId) {
		return this.springDataRepository.findByAthleteId(athleteId).stream().map(this::toDomain).toList();
	}

	@Override
	public List<TrainingPlan> findByCoachId(UUID coachId) {
		return this.springDataRepository.findByCoachId(coachId).stream().map(this::toDomain).toList();
	}

	@Override
	public List<TrainingPlan> findByPeriodizationId(UUID periodizationId) {
		return this.springDataRepository.findByPeriodizationId(periodizationId).stream().map(this::toDomain).toList();
	}

	@Override
	public List<TrainingPlan> findByAthleteIdAndStatus(UUID athleteId, TrainingStatus status) {
		return this.springDataRepository.findByAthleteIdAndStatus(athleteId, status).stream().map(this::toDomain)
			.toList();
	}

	@Override
	public Optional<TrainingPlan> findLatestVersionByAthleteId(UUID athleteId) {
		return this.springDataRepository.findLatestVersionByAthleteId(athleteId).map(this::toDomain);
	}

	@Override
	public List<TrainingPlan> findByPreviousVersionId(UUID previousVersionId) {
		return this.springDataRepository.findByPreviousVersionId(previousVersionId).stream().map(this::toDomain)
			.toList();
	}

	private TrainingPlanJpaEntity toEntity(TrainingPlan domain) {
		TrainingPlanJpaEntity entity = new TrainingPlanJpaEntity();
		entity.setId(domain.getId());
		entity.setAthleteId(domain.getAthleteId());
		entity.setCoachId(domain.getCoachId());
		entity.setPeriodizationId(domain.getPeriodizationId());
		entity.setName(domain.getName());
		entity.setDescription(domain.getDescription());
		entity.setVersion(domain.getVersion());
		entity.setPreviousVersionId(domain.getPreviousVersionId());
		entity.setStatus(domain.getStatus());
		entity.setCreatedAt(domain.getCreatedAt());
		entity.setUpdatedAt(domain.getUpdatedAt());
		entity.setDeleted(domain.isDeleted());
		entity.setDeletedAt(domain.getDeletedAt());
		return entity;
	}

	private TrainingPlan toDomain(TrainingPlanJpaEntity entity) {
		return new TrainingPlan(entity.getId(), entity.getAthleteId(), entity.getCoachId(), entity.getPeriodizationId(),
				entity.getName(), entity.getDescription(), entity.getVersion(), entity.getPreviousVersionId(),
				entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.isDeleted(),
				entity.getDeletedAt());
	}

}
