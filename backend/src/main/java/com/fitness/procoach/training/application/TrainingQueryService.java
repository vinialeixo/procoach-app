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

	private final TrainingOwnershipGuard ownershipGuard;

	public TrainingQueryService(TrainingRepository trainingRepository, TrainingPlanRepository trainingPlanRepository,
			PeriodizationRepository periodizationRepository, TrainingOwnershipGuard ownershipGuard) {
		this.trainingRepository = trainingRepository;
		this.trainingPlanRepository = trainingPlanRepository;
		this.periodizationRepository = periodizationRepository;
		this.ownershipGuard = ownershipGuard;
	}

	/**
	 * Busca um treino pelo id garantindo que pertence a um atleta da carteira
	 * do coach autenticado (T-058). Treino de outro coach resulta em 404 (a
	 * guarda lança) — não vaza a existência do recurso.
	 */
	public Optional<Training> findTrainingById(UUID id, UUID userId) {
		Optional<Training> training = this.trainingRepository.findById(id);
		if (training.isPresent()) {
			this.ownershipGuard.requireResourceOwnedByCoach(training.get().getAthleteId(), userId);
		}
		return training;
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

	/**
	 * Busca um plano pelo id garantindo que pertence a um atleta da carteira
	 * do coach autenticado (T-058). Plano de outro coach resulta em 404 (a
	 * guarda lança) — não vaza a existência do recurso.
	 */
	public Optional<TrainingPlan> findTrainingPlanById(UUID id, UUID userId) {
		Optional<TrainingPlan> plan = this.trainingPlanRepository.findById(id);
		if (plan.isPresent()) {
			this.ownershipGuard.requireResourceOwnedByCoach(plan.get().getAthleteId(), userId);
		}
		return plan;
	}

	public List<TrainingPlan> findTrainingPlansByAthlete(UUID athleteId) {
		return this.trainingPlanRepository.findByAthleteId(athleteId);
	}

	public Optional<TrainingPlan> findLatestPlanVersion(UUID athleteId) {
		return this.trainingPlanRepository.findLatestVersionByAthleteId(athleteId);
	}

	/**
	 * Busca uma periodização pelo id garantindo que pertence a um atleta da
	 * carteira do coach autenticado (T-058). Periodização de outro coach
	 * resulta em 404 (a guarda lança) — não vaza a existência do recurso.
	 */
	public Optional<Periodization> findPeriodizationById(UUID id, UUID userId) {
		Optional<Periodization> periodization = this.periodizationRepository.findById(id);
		if (periodization.isPresent()) {
			this.ownershipGuard.requireResourceOwnedByCoach(periodization.get().getAthleteId(), userId);
		}
		return periodization;
	}

	public List<Periodization> findPeriodizationsByAthlete(UUID athleteId) {
		return this.periodizationRepository.findByAthleteIdAndDeletedFalse(athleteId);
	}

}
