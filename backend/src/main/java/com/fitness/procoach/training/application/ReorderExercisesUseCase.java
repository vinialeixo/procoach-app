package com.fitness.procoach.training.application;

import java.util.List;
import java.util.UUID;

import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReorderExercisesUseCase {

	private final TrainingRepository trainingRepository;

	private final TrainingOwnershipGuard ownershipGuard;

	public ReorderExercisesUseCase(TrainingRepository trainingRepository, TrainingOwnershipGuard ownershipGuard) {
		this.trainingRepository = trainingRepository;
		this.ownershipGuard = ownershipGuard;
	}

	public Training execute(UUID trainingId, UUID userId, List<UUID> orderedPrescriptionIds) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("Training not found: " + trainingId));

		// T-058 (anti-IDOR): o treino precisa pertencer a um atleta da carteira
		// do coach autenticado — treino de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(training.getAthleteId(), userId);

		training.reorderExercises(orderedPrescriptionIds);
		return this.trainingRepository.save(training);
	}

}
