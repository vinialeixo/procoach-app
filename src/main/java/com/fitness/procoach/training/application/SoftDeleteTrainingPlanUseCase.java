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

	public SoftDeleteTrainingPlanUseCase(TrainingPlanRepository trainingPlanRepository) {
		this.trainingPlanRepository = trainingPlanRepository;
	}

	public TrainingPlan execute(UUID planId) {
		TrainingPlan plan = this.trainingPlanRepository.findById(planId)
			.orElseThrow(() -> new IllegalArgumentException("TrainingPlan not found: " + planId));

		plan.softDelete();
		return this.trainingPlanRepository.save(plan);
	}

}
