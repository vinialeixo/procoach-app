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

	public ReorderExercisesUseCase(TrainingRepository trainingRepository) {
		this.trainingRepository = trainingRepository;
	}

	public Training execute(UUID trainingId, List<UUID> orderedPrescriptionIds) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new IllegalArgumentException("Training not found: " + trainingId));

		training.reorderExercises(orderedPrescriptionIds);
		return this.trainingRepository.save(training);
	}

}
