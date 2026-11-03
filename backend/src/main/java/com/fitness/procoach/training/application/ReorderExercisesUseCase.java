package com.fitness.procoach.training.application;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.training.domain.model.ExercisePrescription;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReorderExercisesUseCase {

	private final TrainingRepository trainingRepository;

	private final TrainingOwnershipGuard ownershipGuard;

	private final AuditService auditService;

	private final TrainingNotifier notifier;

	public ReorderExercisesUseCase(TrainingRepository trainingRepository, TrainingOwnershipGuard ownershipGuard,
			AuditService auditService, TrainingNotifier notifier) {
		this.trainingRepository = trainingRepository;
		this.ownershipGuard = ownershipGuard;
		this.auditService = auditService;
		this.notifier = notifier;
	}

	public Training execute(UUID trainingId, UUID userId, List<UUID> orderedPrescriptionIds) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("Training not found: " + trainingId));

		// T-058 (anti-IDOR): o treino precisa pertencer a um atleta da carteira
		// do coach autenticado — treino de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(training.getAthleteId(), userId);

		String before = order(training);
		training.reorderExercises(orderedPrescriptionIds);
		Training saved = this.trainingRepository.save(training);
		this.auditService.record("Training", saved.getId(), saved.getAthleteId(), "TRAINING_EXERCISES_REORDERED",
				userId, AuditData.of("before", before, "after", order(saved)));
		this.notifier.trainingChanged(saved,
				"Seu coach reorganizou a ordem dos exercícios do treino \"" + saved.getName() + "\".");
		return saved;
	}

	/** Ids das prescrições na ordem de execução, separados por vírgula. */
	private static String order(Training training) {
		return training.getExercises().stream()
			.sorted(Comparator.comparingInt(ExercisePrescription::getOrderIndex))
			.map(e -> e.getId().toString())
			.collect(java.util.stream.Collectors.joining(","));
	}
}
