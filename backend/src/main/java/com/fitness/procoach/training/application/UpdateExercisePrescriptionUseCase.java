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
 * Edita uma prescrição de um treino (inclusive ativo). O histórico guarda o antes/depois de cada
 * campo alterado e, se o treino estiver ativo, o atleta é avisado na mesma transação. Edição sem
 * nenhuma mudança não grava histórico nem aviso.
 */
@Service
@Transactional
public class UpdateExercisePrescriptionUseCase {

	private final TrainingRepository trainingRepository;

	private final TrainingOwnershipGuard ownershipGuard;

	private final AuditService auditService;

	private final TrainingNotifier notifier;

	public UpdateExercisePrescriptionUseCase(TrainingRepository trainingRepository,
			TrainingOwnershipGuard ownershipGuard, AuditService auditService, TrainingNotifier notifier) {
		this.trainingRepository = trainingRepository;
		this.ownershipGuard = ownershipGuard;
		this.auditService = auditService;
		this.notifier = notifier;
	}

	public Training execute(UUID trainingId, UUID prescriptionId, UUID userId, UUID exerciseId, String exerciseName,
			int sets, int repetitions, BigDecimal load, IntensityType intensityType, BigDecimal intensityValue,
			int restSeconds, String notes) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("Training not found: " + trainingId));
		// T-058 (anti-IDOR): treino de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(training.getAthleteId(), userId);
		ExercisePrescription before = find(training, prescriptionId);

		training.updateExercise(prescriptionId, exerciseId, exerciseName, sets, repetitions, load, intensityType,
				intensityValue, restSeconds, notes);

		ExercisePrescription after = find(training, prescriptionId);
		List<PrescriptionChanges.Change> changes = PrescriptionChanges.between(before, after);
		if (changes.isEmpty()) {
			return training;
		}
		Training saved = this.trainingRepository.save(training);
		this.auditService.record("Training", saved.getId(), saved.getAthleteId(), "TRAINING_EXERCISE_UPDATED", userId,
				PrescriptionChanges.auditData(prescriptionId, changes));
		this.notifier.trainingChanged(saved, "Seu coach ajustou \"" + after.getExerciseName() + "\" no treino \""
				+ saved.getName() + "\": " + PrescriptionChanges.describe(changes) + ".");
		return saved;
	}

	private static ExercisePrescription find(Training training, UUID prescriptionId) {
		return training.getExercises()
			.stream()
			.filter(e -> e.getId().equals(prescriptionId))
			.findFirst()
			.orElseThrow(() -> new TrainingResourceNotFoundException(
					"Exercise prescription not found: " + prescriptionId));
	}

}
