package com.fitness.procoach.training.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.fitness.procoach.training.domain.event.ExerciseAddedEvent;
import com.fitness.procoach.training.domain.model.ExercisePrescription;
import com.fitness.procoach.training.domain.model.IntensityType;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddExerciseToTrainingUseCase {

	private final TrainingRepository trainingRepository;

	private final ApplicationEventPublisher eventPublisher;

	private final TrainingOwnershipGuard ownershipGuard;

	public AddExerciseToTrainingUseCase(TrainingRepository trainingRepository,
			ApplicationEventPublisher eventPublisher, TrainingOwnershipGuard ownershipGuard) {
		this.trainingRepository = trainingRepository;
		this.eventPublisher = eventPublisher;
		this.ownershipGuard = ownershipGuard;
	}

	public Training execute(UUID trainingId, UUID userId, UUID exerciseId, String exerciseName,
			boolean customExercise, int orderIndex, int sets, int repetitions, BigDecimal load,
			IntensityType intensityType, BigDecimal intensityValue, int restSeconds, String notes) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new IllegalArgumentException("Training not found: " + trainingId));

		// T-058 (anti-IDOR): o treino precisa pertencer a um atleta da carteira
		// do coach autenticado — treino de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(training.getAthleteId(), userId);

		ExercisePrescription prescription = ExercisePrescription.create(exerciseId, exerciseName, customExercise,
				orderIndex, sets, repetitions, load, intensityType, intensityValue, restSeconds, notes);

		training.addExercise(prescription);
		Training saved = this.trainingRepository.save(training);

		this.eventPublisher.publishEvent(new ExerciseAddedEvent(saved.getId(), prescription.getId(),
				prescription.getExerciseId(), prescription.getExerciseName(), prescription.isCustomExercise(),
				prescription.getSets(), prescription.getRepetitions()));

		return saved;
	}

}
