package com.fitness.procoach.training.application;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.training.domain.model.ExercisePrescription;
import com.fitness.procoach.training.domain.model.IntensityType;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.repository.TrainingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remove uma prescrição de um treino (inclusive ativo), com histórico e aviso ao atleta quando o
 * treino já é visível para ele.
 */
@Service
@Transactional
public class RemoveExerciseFromTrainingUseCase {

	private final TrainingRepository trainingRepository;

	private final TrainingOwnershipGuard ownershipGuard;

	private final AuditService auditService;

	private final TrainingNotifier notifier;

	public RemoveExerciseFromTrainingUseCase(TrainingRepository trainingRepository,
			TrainingOwnershipGuard ownershipGuard, AuditService auditService, TrainingNotifier notifier) {
		this.trainingRepository = trainingRepository;
		this.ownershipGuard = ownershipGuard;
		this.auditService = auditService;
		this.notifier = notifier;
	}

	public Training execute(UUID trainingId, UUID prescriptionId, UUID userId) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("Training not found: " + trainingId));
		// T-058 (anti-IDOR): treino de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(training.getAthleteId(), userId);
		ExercisePrescription removed = training.getExercises()
			.stream()
			.filter(e -> e.getId().equals(prescriptionId))
			.findFirst()
			.orElseThrow(() -> new TrainingResourceNotFoundException(
					"Exercise prescription not found: " + prescriptionId));

		training.removeExercise(prescriptionId);

		Training saved = this.trainingRepository.save(training);
		this.auditService.record("Training", saved.getId(), saved.getAthleteId(), "TRAINING_EXERCISE_REMOVED", userId,
				AuditData.of("prescriptionId", removed.getId(), "exerciseId", removed.getExerciseId(), "exerciseName",
						removed.getExerciseName()));
		this.notifier.trainingChanged(saved,
				"Seu coach removeu \"" + removed.getExerciseName() + "\" do treino \"" + saved.getName() + "\".");
		return saved;
	}

}
