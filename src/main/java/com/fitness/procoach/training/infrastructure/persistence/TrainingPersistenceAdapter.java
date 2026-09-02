package com.fitness.procoach.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.ExercisePrescription;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingStatus;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

import org.springframework.stereotype.Component;

@Component
public class TrainingPersistenceAdapter implements TrainingRepository {

	private final TrainingSpringDataRepository springDataRepository;

	public TrainingPersistenceAdapter(TrainingSpringDataRepository springDataRepository) {
		this.springDataRepository = springDataRepository;
	}

	@Override
	public Training save(Training training) {
		TrainingJpaEntity entity = toEntity(training);
		TrainingJpaEntity saved = this.springDataRepository.save(entity);
		return toDomain(saved);
	}

	@Override
	public Optional<Training> findById(UUID id) {
		return this.springDataRepository.findById(id).map(this::toDomain);
	}

	@Override
	public List<Training> findByPeriodizationId(UUID periodizationId) {
		return this.springDataRepository.findByPeriodizationId(periodizationId).stream().map(this::toDomain).toList();
	}

	@Override
	public List<Training> findByAthleteId(UUID athleteId) {
		return this.springDataRepository.findByAthleteId(athleteId).stream().map(this::toDomain).toList();
	}

	@Override
	public List<Training> findByCoachId(UUID coachId) {
		return this.springDataRepository.findByCoachId(coachId).stream().map(this::toDomain).toList();
	}

	@Override
	public List<Training> findByAthleteIdAndStatus(UUID athleteId, TrainingStatus status) {
		return this.springDataRepository.findByAthleteIdAndStatus(athleteId, status).stream().map(this::toDomain)
			.toList();
	}

	@Override
	public List<Training> findByPeriodizationIdAndDeletedFalse(UUID periodizationId) {
		return this.springDataRepository.findByPeriodizationIdAndDeletedFalse(periodizationId).stream()
			.map(this::toDomain)
			.toList();
	}

	private TrainingJpaEntity toEntity(Training domain) {
		TrainingJpaEntity entity = new TrainingJpaEntity();
		entity.setId(domain.getId());
		entity.setPeriodizationId(domain.getPeriodizationId());
		entity.setAthleteId(domain.getAthleteId());
		entity.setCoachId(domain.getCoachId());
		entity.setName(domain.getName());
		entity.setDescription(domain.getDescription());
		entity.setWeekNumber(domain.getWeekNumber());
		entity.setDayOfWeek(domain.getDayOfWeek());
		entity.setStatus(domain.getStatus());
		entity.setCreatedAt(domain.getCreatedAt());
		entity.setUpdatedAt(domain.getUpdatedAt());
		entity.setDeleted(domain.isDeleted());
		entity.setDeletedAt(domain.getDeletedAt());

		List<ExercisePrescriptionJpaEntity> exerciseEntities = domain.getExercises().stream().map(e -> {
			ExercisePrescriptionJpaEntity ee = new ExercisePrescriptionJpaEntity();
			ee.setId(e.getId());
			ee.setTrainingId(domain.getId());
			ee.setExerciseId(e.getExerciseId());
			ee.setExerciseName(e.getExerciseName());
			ee.setCustomExercise(e.isCustomExercise());
			ee.setOrderIndex(e.getOrderIndex());
			ee.setSets(e.getSets());
			ee.setRepetitions(e.getRepetitions());
			ee.setLoad(e.getLoad());
			ee.setIntensityType(e.getIntensityType());
			ee.setIntensityValue(e.getIntensityValue());
			ee.setRestSeconds(e.getRestSeconds());
			ee.setNotes(e.getNotes());
			return ee;
		}).toList();

		entity.setExercises(exerciseEntities);
		return entity;
	}

	private Training toDomain(TrainingJpaEntity entity) {
		List<ExercisePrescription> exercises = entity.getExercises().stream().map(ee -> new ExercisePrescription(
				ee.getId(), ee.getExerciseId(), ee.getExerciseName(), ee.isCustomExercise(), ee.getOrderIndex(),
				ee.getSets(), ee.getRepetitions(), ee.getLoad(), ee.getIntensityType(), ee.getIntensityValue(),
				ee.getRestSeconds(), ee.getNotes())).toList();

		return new Training(entity.getId(), entity.getPeriodizationId(), entity.getAthleteId(), entity.getCoachId(),
				entity.getName(), entity.getDescription(), entity.getWeekNumber(), entity.getDayOfWeek(),
				entity.getStatus(), exercises, entity.getCreatedAt(), entity.getUpdatedAt(), entity.isDeleted(),
				entity.getDeletedAt());
	}

}
