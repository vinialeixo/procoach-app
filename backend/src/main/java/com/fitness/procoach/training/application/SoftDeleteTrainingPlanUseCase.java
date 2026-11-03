package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.training.domain.model.TrainingPlan;
import com.fitness.procoach.training.domain.repository.TrainingPlanRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SoftDeleteTrainingPlanUseCase {

	private final TrainingPlanRepository trainingPlanRepository;

	private final TrainingOwnershipGuard ownershipGuard;

	public SoftDeleteTrainingPlanUseCase(TrainingPlanRepository trainingPlanRepository,
			TrainingOwnershipGuard ownershipGuard) {
		this.trainingPlanRepository = trainingPlanRepository;
		this.ownershipGuard = ownershipGuard;
	}

	public TrainingPlan execute(UUID planId, UUID userId) {
		TrainingPlan plan = this.trainingPlanRepository.findById(planId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("TrainingPlan not found: " + planId));

		// T-058 (anti-IDOR): o plano precisa pertencer a um atleta da carteira
		// do coach autenticado — plano de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(plan.getAthleteId(), userId);

		plan.softDelete();
		return this.trainingPlanRepository.save(plan);
	}

}
