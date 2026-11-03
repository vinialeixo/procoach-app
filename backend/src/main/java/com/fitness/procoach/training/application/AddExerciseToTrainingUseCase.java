package com.fitness.procoach.training.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.fitness.procoach.training.domain.event.ExerciseAddedEvent;
import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
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

	private final AuditService auditService;

	public AddExerciseToTrainingUseCase(TrainingRepository trainingRepository,
			ApplicationEventPublisher eventPublisher, TrainingOwnershipGuard ownershipGuard,
			AuditService auditService) {
		this.trainingRepository = trainingRepository;
		this.eventPublisher = eventPublisher;
		this.ownershipGuard = ownershipGuard;
		this.auditService = auditService;
	}

	public Training execute(UUID trainingId, UUID userId, UUID exerciseId, String exerciseName,
			boolean customExercise, int orderIndex, int sets, int repetitions, BigDecimal load,
			IntensityType intensityType, BigDecimal intensityValue, int restSeconds, String notes) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("Training not found: " + trainingId));

		// T-058 (anti-IDOR): o treino precisa pertencer a um atleta da carteira
		// do coach autenticado — treino de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(training.getAthleteId(), userId);

		ExercisePrescription prescription = ExercisePrescription.create(exerciseId, exerciseName, customExercise,
				orderIndex, sets, repetitions, load, intensityType, intensityValue, restSeconds, notes);

		training.addExercise(prescription);
		Training saved = this.trainingRepository.save(training);

		// Observações livres (notes) ficam de fora do histórico: podem conter dados pessoais.
		this.auditService.record("Training", saved.getId(), saved.getAthleteId(), "TRAINING_EXERCISE_ADDED", userId,
				AuditData.of("prescriptionId", prescription.getId(), "exerciseId", prescription.getExerciseId(),
						"exerciseName", prescription.getExerciseName(), "custom", prescription.isCustomExercise(),
						"sets", prescription.getSets(), "repetitions", prescription.getRepetitions(), "load",
						prescription.getLoad(), "intensityType", prescription.getIntensityType(), "intensityValue",
						prescription.getIntensityValue(), "restSeconds", prescription.getRestSeconds()));
		this.eventPublisher.publishEvent(new ExerciseAddedEvent(saved.getId(), prescription.getId(),
				prescription.getExerciseId(), prescription.getExerciseName(), prescription.isCustomExercise(),
				prescription.getSets(), prescription.getRepetitions()));

		return saved;
	}

}
