package com.fitness.procoach.training.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.model.TrainingStatus;
import com.fitness.procoach.training.domain.repository.PeriodizationRepository;
import com.fitness.procoach.training.domain.repository.TrainingPlanRepository;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingQueryService {

	private final TrainingRepository trainingRepository;

	private final TrainingPlanRepository trainingPlanRepository;

	private final PeriodizationRepository periodizationRepository;

	public TrainingQueryService(TrainingRepository trainingRepository, TrainingPlanRepository trainingPlanRepository,
			PeriodizationRepository periodizationRepository) {
		this.trainingRepository = trainingRepository;
		this.trainingPlanRepository = trainingPlanRepository;
		this.periodizationRepository = periodizationRepository;
	}

	public Optional<Training> findTrainingById(UUID id) {
		return this.trainingRepository.findById(id);
	}

	public List<Training> findTrainingsByPeriodization(UUID periodizationId) {
		return this.trainingRepository.findByPeriodizationIdAndDeletedFalse(periodizationId);
	}

	public List<Training> findTrainingsByAthlete(UUID athleteId) {
		return this.trainingRepository.findByAthleteId(athleteId);
	}

	public List<Training> findTrainingsByAthleteAndStatus(UUID athleteId, TrainingStatus status) {
		return this.trainingRepository.findByAthleteIdAndStatus(athleteId, status);
	}

	public Optional<TrainingPlan> findTrainingPlanById(UUID id) {
		return this.trainingPlanRepository.findById(id);
	}

	public List<TrainingPlan> findTrainingPlansByAthlete(UUID athleteId) {
		return this.trainingPlanRepository.findByAthleteId(athleteId);
	}

	public Optional<TrainingPlan> findLatestPlanVersion(UUID athleteId) {
		return this.trainingPlanRepository.findLatestVersionByAthleteId(athleteId);
	}

	public Optional<Periodization> findPeriodizationById(UUID id) {
		return this.periodizationRepository.findById(id);
	}

	public List<Periodization> findPeriodizationsByAthlete(UUID athleteId) {
		return this.periodizationRepository.findByAthleteIdAndDeletedFalse(athleteId);
	}

}
