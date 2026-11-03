package com.fitness.procoach.training.application;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fitness.procoach.library.application.ExerciseApplicationService;
import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.model.PeriodizationStatus;
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

	/** Status que o atleta enxerga: o coach já liberou o conteúdo. */
	private static final Set<TrainingStatus> ATHLETE_VISIBLE = EnumSet.of(TrainingStatus.ACTIVE,
			TrainingStatus.FINISHED);

	private final ExerciseApplicationService exerciseService;

	private final TrainingRepository trainingRepository;

	private final TrainingPlanRepository trainingPlanRepository;

	private final PeriodizationRepository periodizationRepository;

	private final TrainingOwnershipGuard ownershipGuard;

	public TrainingQueryService(TrainingRepository trainingRepository, TrainingPlanRepository trainingPlanRepository,
			PeriodizationRepository periodizationRepository, TrainingOwnershipGuard ownershipGuard,
			ExerciseApplicationService exerciseService) {
		this.exerciseService = exerciseService;
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

	/**
	 * Treinos do atleta sem os removidos (soft-delete). Na visão do próprio atleta
	 * só aparece o que o coach já liberou: treinos ACTIVE ou FINISHED — rascunho e
	 * em revisão ficam só com o coach.
	 */
	public List<Training> findTrainingsByAthlete(UUID athleteId, TrainingStatus status, boolean athleteView) {
		return findTrainingsByAthlete(athleteId, status, null, athleteView);
	}

	/** Igual ao anterior, com filtro opcional por versão do plano ({@code planId}). */
	public List<Training> findTrainingsByAthlete(UUID athleteId, TrainingStatus status, UUID planId,
			boolean athleteView) {
		List<Training> base = (status != null) ? findTrainingsByAthleteAndStatus(athleteId, status)
				: findTrainingsByAthlete(athleteId);
		return base.stream()
			.filter(t -> !t.isDeleted())
			.filter(t -> planId == null || planId.equals(t.getPlanId()))
			.filter(t -> !athleteView || ATHLETE_VISIBLE.contains(t.getStatus()))
			.toList();
	}

	/** Planos do atleta sem os removidos; o atleta vê só ACTIVE e FINISHED. */
	public List<TrainingPlan> findTrainingPlansByAthlete(UUID athleteId, boolean athleteView) {
		return findTrainingPlansByAthlete(athleteId).stream()
			.filter(p -> !p.isDeleted())
			.filter(p -> !athleteView || ATHLETE_VISIBLE.contains(p.getStatus()))
			.toList();
	}

	/**
	 * Plano corrente. Coach: a versão mais recente, seja qual for o status. Atleta:
	 * a versão ACTIVE mais recente (o que o coach liberou), sem rascunhos.
	 */
	public Optional<TrainingPlan> findLatestPlanVersion(UUID athleteId, boolean athleteView) {
		if (!athleteView) {
			return findLatestPlanVersion(athleteId);
		}
		return findTrainingPlansByAthlete(athleteId, true).stream()
			.filter(p -> p.getStatus() == TrainingStatus.ACTIVE)
			.max(Comparator.comparingInt(TrainingPlan::getVersion));
	}

	/** Periodizações do atleta; o atleta vê só as ACTIVE e FINISHED. */
	public List<Periodization> findPeriodizationsByAthlete(UUID athleteId, boolean athleteView) {
		return findPeriodizationsByAthlete(athleteId).stream()
			.filter(p -> !athleteView || p.getStatus() == PeriodizationStatus.ACTIVE
					|| p.getStatus() == PeriodizationStatus.FINISHED)
			.toList();
	}

	/**
	 * Status de versão dos exercícios da biblioteca prescritos nos treinos informados, por id do
	 * exercício (T-066). Exercício personalizado do coach não tem versão e fica de fora.
	 */
	public java.util.Map<UUID, ExerciseApplicationService.ExerciseVersionStatus> exerciseVersionStatuses(
			java.util.Collection<Training> trainings) {
		List<UUID> exerciseIds = trainings.stream()
			.flatMap(t -> t.getExercises().stream())
			.filter(e -> !e.isCustomExercise() && e.getExerciseId() != null)
			.map(e -> e.getExerciseId())
			.toList();
		return exerciseIds.isEmpty() ? java.util.Map.of() : this.exerciseService.versionStatuses(exerciseIds);
	}

	public List<Periodization> findPeriodizationsByAthlete(UUID athleteId) {
		return this.periodizationRepository.findByAthleteIdAndDeletedFalse(athleteId);
	}

}
