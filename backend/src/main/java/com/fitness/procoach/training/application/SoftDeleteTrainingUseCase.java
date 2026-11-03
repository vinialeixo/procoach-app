package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.repository.TrainingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SoftDeleteTrainingUseCase {

	private final TrainingRepository trainingRepository;

	private final TrainingOwnershipGuard ownershipGuard;

	private final AuditService auditService;

	public SoftDeleteTrainingUseCase(TrainingRepository trainingRepository, TrainingOwnershipGuard ownershipGuard,
			AuditService auditService) {
		this.trainingRepository = trainingRepository;
		this.ownershipGuard = ownershipGuard;
		this.auditService = auditService;
	}

	public Training execute(UUID trainingId, UUID userId) {
		Training training = this.trainingRepository.findById(trainingId)
			.orElseThrow(() -> new TrainingResourceNotFoundException("Training not found: " + trainingId));

		// T-058 (anti-IDOR): o treino precisa pertencer a um atleta da carteira
		// do coach autenticado — treino de outro coach resulta em 404.
		this.ownershipGuard.requireResourceOwnedByCoach(training.getAthleteId(), userId);

		training.softDelete();
		Training saved = this.trainingRepository.save(training);
		this.auditService.record("Training", saved.getId(), saved.getAthleteId(), "TRAINING_DELETED", userId,
				AuditData.of());
		return saved;
	}

}
