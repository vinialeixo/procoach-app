package com.fitness.procoach.assessment.application;

import com.fitness.procoach.assessment.domain.Assessment;
import com.fitness.procoach.assessment.domain.AssessmentId;
import com.fitness.procoach.assessment.domain.AssessmentRepository;
import com.fitness.procoach.assessment.domain.BodyMeasurements;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Application service do módulo assessment. Orquestra o domínio e a porta de
 * persistência. Framework-agnostic: registrado como bean pela configuração da
 * camada de infraestrutura.
 */
public class AssessmentApplicationService {

	private final AssessmentRepository repository;

	public AssessmentApplicationService(AssessmentRepository repository) {
		this.repository = repository;
	}

	/**
	 * Registra uma nova avaliação física (US-004). A data é obrigatória e o
	 * agregado é imutável: cada avaliação é um novo registro, nunca sobrescreve
	 * a anterior.
	 */
	public Assessment registerAssessment(RegisterAssessmentCommand command) {
		Assessment assessment = Assessment.create(
				command.athleteId(),
				command.coachId(),
				command.assessmentDate(),
				new BodyMeasurements(command.weightKg(), command.heightCm(),
						command.bodyFatPercentage(), command.muscleMassKg(),
						command.waistCm(), command.hipCm()));
		return repository.save(assessment);
	}

	/**
	 * Histórico de avaliações de um atleta em ordem cronológica (US-004).
	 */
	public List<Assessment> history(UUID athleteId) {
		return repository.findByAthleteIdOrderByAssessmentDateAsc(athleteId);
	}

	public Assessment findById(AssessmentId id) {
		return repository.findById(id)
				.orElseThrow(() -> new AssessmentNotFoundException(id.value()));
	}

	public record RegisterAssessmentCommand(UUID athleteId, UUID coachId, LocalDate assessmentDate,
			BigDecimal weightKg, BigDecimal heightCm, BigDecimal bodyFatPercentage,
			BigDecimal muscleMassKg, BigDecimal waistCm, BigDecimal hipCm) {
	}
}
